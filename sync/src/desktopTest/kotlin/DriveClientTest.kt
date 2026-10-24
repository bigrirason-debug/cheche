package io.github.bigrirason_debug.platen.sync

import io.github.bigrirason_debug.platen.sync.auth.AccessTokenSource
import io.github.bigrirason_debug.platen.sync.drive.DriveClient
import io.github.bigrirason_debug.platen.sync.drive.DriveException
import io.github.bigrirason_debug.platen.sync.drive.PLATEN_MIME
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DriveClientTest {
    private class FakeTokens : AccessTokenSource {
        var current = "t1"
        var refreshes = 0
        override suspend fun accessToken() = current
        override suspend fun refreshAccessToken(): String { refreshes++; current = "t${refreshes + 1}"; return current }
    }

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK): HttpResponseData = respond(body, status, jsonHeaders)

    private fun config(chunk: Int = 8 * 1024 * 1024) = SyncConfig(chunkBytes = chunk, deviceId = "dev-A")

    private fun client(
        tokens: FakeTokens = FakeTokens(),
        config: SyncConfig = config(),
        sleeps: MutableList<Long> = mutableListOf(),
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = DriveClient(MockEngine(handler), tokens, config, random = Random(1), sleeper = { sleeps += it })

    @Test
    fun rootFolderFoundOrCreated() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val found = client { req ->
            requests += req
            json("""{"files":[{"id":"folder-existing","name":"Platen"}]}""")
        }
        assertEquals("folder-existing", found.ensureRootFolder())
        assertEquals(1, requests.size)
        assertTrue(requests[0].url.parameters["q"]!!.contains("name = 'Platen'"))

        requests.clear()
        val created = client { req ->
            requests += req
            if (req.method == HttpMethod.Get) json("""{"files":[]}""") else json("""{"id":"folder-new"}""")
        }
        assertEquals("folder-new", created.ensureRootFolder())
        assertEquals(2, requests.size)
        assertEquals(HttpMethod.Post, requests[1].method)
        val body = String(requests[1].body.toByteArray())
        assertTrue(body.contains("application/vnd.google-apps.folder") && body.contains("\"Platen\""), body)
    }

    @Test
    fun multipartCreateHasTwoPartsAndMime() = runTest {
        var captured: HttpRequestData? = null
        val c = client { req ->
            captured = req
            json("""{"id":"f1","md5Checksum":"abc","version":"3"}""")
        }
        val media = ByteArray(1000) { it.toByte() }
        val f = c.create("note.platen", media, "folder1", mapOf("gen" to "1", "deviceId" to "dev-A", "baseMd5" to ""))
        assertEquals("f1", f.id)
        val req = captured!!
        assertEquals(HttpMethod.Post, req.method)
        assertEquals("/upload/drive/v3/files", req.url.encodedPath)
        assertEquals("multipart", req.url.parameters["uploadType"])
        val ct = req.body.contentType.toString()
        assertTrue(ct.startsWith("multipart/related; boundary="), ct)
        val boundary = ct.substringAfter("boundary=")
        val bytes = req.body.toByteArray()
        val text = String(bytes, Charsets.ISO_8859_1)
        val parts = text.split("--$boundary").filter { it.isNotBlank() && it != "--" }
        assertEquals(2, parts.size, "metadata part + media part")
        assertTrue(parts[0].contains("Content-Type: application/json"))
        assertTrue(parts[0].contains("\"mimeType\":\"$PLATEN_MIME\""))
        assertTrue(parts[0].contains("\"parents\":[\"folder1\"]"))
        assertTrue(parts[0].contains("\"appProperties\":{\"gen\":\"1\",\"deviceId\":\"dev-A\",\"baseMd5\":\"\"}"))
        assertTrue(parts[1].startsWith("\r\nContent-Type: $PLATEN_MIME\r\n\r\n"))
        assertTrue(text.endsWith("\r\n--$boundary--"))
        val mediaStart = text.indexOf("Content-Type: $PLATEN_MIME\r\n\r\n") + "Content-Type: $PLATEN_MIME\r\n\r\n".length
        assertTrue(bytes.copyOfRange(mediaStart, mediaStart + media.size).contentEquals(media))
    }

    @Test
    fun resumableUploadSplitsIntoChunks() = runTest {
        val five = 5 * 1024 * 1024
        val requests = mutableListOf<HttpRequestData>()
        val c = client(config = config(chunk = five)) { req ->
            requests += req
            when {
                req.url.parameters["uploadType"] == "resumable" ->
                    respond("", HttpStatusCode.OK, headersOf(HttpHeaders.Location, listOf("https://www.googleapis.com/upload/session/xyz")))
                req.method == HttpMethod.Put && req.headers[HttpHeaders.ContentRange]!!.startsWith("bytes 0-") ->
                    respond("", HttpStatusCode.fromValue(308), headersOf("Range", listOf("bytes=0-${five - 1}")))
                req.method == HttpMethod.Put -> json("""{"id":"f2","md5Checksum":"m","version":"9"}""")
                else -> error("unexpected ${req.method} ${req.url}")
            }
        }
        val data = ByteArray(five + 1) { 7 }
        val f = c.update("f2", data, emptyMap())
        assertEquals("9", f.version)
        assertEquals(3, requests.size, "session start + 2 chunk PUTs")
        assertEquals(HttpMethod.Patch, requests[0].method)
        assertEquals("/upload/drive/v3/files/f2", requests[0].url.encodedPath)
        assertEquals(PLATEN_MIME, requests[0].headers["X-Upload-Content-Type"])
        assertEquals((five + 1).toString(), requests[0].headers["X-Upload-Content-Length"])
        assertEquals("bytes 0-${five - 1}/${five + 1}", requests[1].headers[HttpHeaders.ContentRange])
        assertEquals(five, requests[1].body.toByteArray().size)
        assertEquals("bytes $five-$five/${five + 1}", requests[2].headers[HttpHeaders.ContentRange])
        assertEquals(1, requests[2].body.toByteArray().size)
    }

    @Test
    fun unauthorizedRefreshesOnceAndRetries() = runTest {
        val tokens = FakeTokens()
        val auths = mutableListOf<String?>()
        val c = client(tokens) { req ->
            auths += req.headers[HttpHeaders.Authorization]
            if (auths.size == 1) json("""{"error":{"code":401}}""", HttpStatusCode.Unauthorized)
            else json("""{"id":"f1","version":"1"}""")
        }
        assertEquals("f1", c.getFile("f1")!!.id)
        assertEquals(listOf<String?>("Bearer t1", "Bearer t2"), auths)
        assertEquals(1, tokens.refreshes)

        // 두 번 연속 401 이면 갱신은 1회만, 예외.
        val tokens2 = FakeTokens()
        val c2 = client(tokens2) { json("""{}""", HttpStatusCode.Unauthorized) }
        assertFailsWith<DriveException> { c2.getFile("f1") }
        assertEquals(1, tokens2.refreshes)
    }

    @Test
    fun rateLimitBacksOffExponentially() = runTest {
        val sleeps = mutableListOf<Long>()
        var calls = 0
        val c = client(sleeps = sleeps) {
            calls++
            if (calls <= 3) json("""{"error":{"code":429}}""", HttpStatusCode.TooManyRequests)
            else json("""{"startPageToken":"42"}""")
        }
        assertEquals("42", c.startPageToken())
        assertEquals(4, calls)
        assertEquals(3, sleeps.size)
        // 1s·2s·4s + 지터(<1s)
        assertTrue(sleeps[0] in 1_000L until 2_000L && sleeps[1] in 2_000L until 3_000L && sleeps[2] in 4_000L until 5_000L, sleeps.toString())

        // 403 userRateLimitExceeded 도 재시도, 소진 시 예외: 1 + 5 재시도 = 6회.
        val sleeps2 = mutableListOf<Long>()
        var calls2 = 0
        val c2 = client(sleeps = sleeps2) {
            calls2++
            json("""{"error":{"code":403,"errors":[{"reason":"userRateLimitExceeded"}]}}""", HttpStatusCode.Forbidden)
        }
        val e = assertFailsWith<DriveException> { c2.startPageToken() }
        assertEquals(403, e.status)
        assertEquals(6, calls2)
        assertEquals(5, sleeps2.size)

        // 권한 403 은 즉시 실패.
        var calls3 = 0
        val c3 = client { calls3++; json("""{"error":{"code":403,"errors":[{"reason":"insufficientPermissions"}]}}""", HttpStatusCode.Forbidden) }
        assertFailsWith<DriveException> { c3.startPageToken() }
        assertEquals(1, calls3)
    }

    @Test
    fun getFileReturnsNullOn404AndTrashSendsPatch() = runTest {
        var patched: HttpRequestData? = null
        val c = client { req ->
            when {
                req.method == HttpMethod.Patch -> { patched = req; json("""{"id":"f1","trashed":true}""") }
                else -> json("""{"error":{"code":404}}""", HttpStatusCode.NotFound)
            }
        }
        assertEquals(null, c.getFile("missing"))
        c.trash("f1")
        assertEquals("/drive/v3/files/f1", patched!!.url.encodedPath)
        assertTrue(String(patched!!.body.toByteArray()).contains("\"trashed\":true"))
    }

    @Test
    fun changesListFollowsPages() = runTest {
        val tokensSeen = mutableListOf<String?>()
        val c = client { req ->
            tokensSeen += req.url.parameters["pageToken"]
            if (tokensSeen.size == 1) json("""{"nextPageToken":"p2","changes":[{"fileId":"a","removed":true}]}""")
            else json("""{"newStartPageToken":"99","changes":[{"fileId":"b","file":{"id":"b","md5Checksum":"m"}}]}""")
        }
        val page = c.listChanges("p1")
        assertEquals(listOf<String?>("p1", "p2"), tokensSeen)
        assertEquals(2, page.changes.size)
        assertEquals("99", page.newStartPageToken)
    }
}

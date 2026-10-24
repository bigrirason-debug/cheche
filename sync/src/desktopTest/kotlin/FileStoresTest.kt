package io.github.bigrirason_debug.platen.sync

import io.github.bigrirason_debug.platen.sync.auth.FileDeviceIdStore
import io.github.bigrirason_debug.platen.sync.auth.FileTokenStore
import io.github.bigrirason_debug.platen.sync.auth.OAuthClientConfig
import io.github.bigrirason_debug.platen.sync.auth.OAuthClientConfigMissingException
import io.github.bigrirason_debug.platen.sync.auth.OAuthTokens
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileStoresTest {
    @Test
    fun tokenFileIsOwnerOnlyAndMarkedPlain() = runTest {
        val dir = Files.createTempDirectory("platen-tokens").toFile()
        try {
            val file = File(dir, "tokens.json")
            val store = FileTokenStore(file)
            assertNull(store.load())
            store.save(OAuthTokens("at", "rt", 123L, listOf("s")))
            assertTrue(file.readText().contains("\"storage\": \"plain\""))
            assertEquals(OAuthTokens("at", "rt", 123L, listOf("s")), store.load())
            runCatching { Files.getPosixFilePermissions(file.toPath()) }.getOrNull()?.let { perms ->
                assertEquals(setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE), perms)
            }
            store.clear()
            assertNull(store.load())

            val id1 = FileDeviceIdStore(File(dir, "device-id")).loadOrCreate()
            val id2 = FileDeviceIdStore(File(dir, "device-id")).loadOrCreate()
            assertEquals(id1, id2)
            assertTrue(id1.length >= 32)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun missingOAuthClientFileGivesGuidance() {
        val paths = DesktopSyncPaths(File("/nonexistent/platen-test-dir"))
        val e = assertFailsWith<OAuthClientConfigMissingException> { OAuthClientConfig.loadDesktop(paths) }
        assertTrue(e.message!!.contains("oauth-client.json") && e.message!!.contains("데스크톱 앱"))
    }
}

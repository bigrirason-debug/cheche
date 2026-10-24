package io.github.bigrirason_debug.platen.sync

/**
 * 동기화 상수(PLAN §4.6, sync-google §4.5/§4.7/§8.1 R5).
 *
 * @param pollIntervalMillis 포그라운드 `changes.list` 폴링 간격. 60s = 1회/분/기기 → 하루 ≤1,440회(§4.7).
 * @param debounceMillis 마지막 로컬 저장 후 업로드까지의 디바운스. 30s(§8.1 R5: 자동 저장 폭주 → 리비전 100개 조기 삭제 완화).
 * @param chunkBytes resumable 업로드 청크. 256 KiB 배수여야 함(§4.5, google-api-python-client media.md). 8 MiB.
 * @param simpleUploadMaxBytes 이 크기 이하는 multipart 1회 업로드, 초과는 resumable(§4.5: 5 MB 권장 상한 [추정]).
 * @param deviceId 설치 시 1회 생성되는 기기 식별자(충돌 사본 이름·appProperties.deviceId).
 */
data class SyncConfig(
    val pollIntervalMillis: Long = 60_000L,
    val debounceMillis: Long = 30_000L,
    val chunkBytes: Int = 8 * 1024 * 1024,
    val simpleUploadMaxBytes: Long = 5L * 1024 * 1024,
    val deviceId: String,
) {
    init {
        require(chunkBytes > 0 && chunkBytes % (256 * 1024) == 0) { "chunkBytes must be a multiple of 256 KiB" }
        require(deviceId.isNotBlank()) { "deviceId required" }
    }
}

/** 주입 가능한 시계(테스트는 가짜 시계). */
fun interface SyncClock {
    fun nowMillis(): Long
}

/** 설치 시 생성되는 deviceId 저장소(TokenStore 옆). */
interface DeviceIdStore {
    /** 저장된 deviceId 를 돌려주거나 없으면 생성·저장한다. */
    suspend fun loadOrCreate(): String
}

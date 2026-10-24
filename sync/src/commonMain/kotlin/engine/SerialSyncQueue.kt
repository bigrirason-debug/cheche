package io.github.bigrirason_debug.platen.sync.engine

import io.github.bigrirason_debug.platen.sync.SyncConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 큐에 들어가는 작업(영속화 가능). */
@Serializable
sealed class SyncJob {
    /** 지정 노트(비어 있으면 전부) 업로드. */
    @Serializable @SerialName("push")
    data class Push(val noteIds: Set<String> = emptySet()) : SyncJob()

    /** 원격 변경 수신. */
    @Serializable @SerialName("pull")
    data object Pull : SyncJob()
}

/** 작업 영속화(앱 종료 시 재개, sync-google §5.5 / D-005 교훈). */
interface QueueStore {
    suspend fun load(): List<SyncJob>
    suspend fun save(jobs: List<SyncJob>)
}

class InMemoryQueueStore(initial: List<SyncJob> = emptyList()) : QueueStore {
    var jobs: List<SyncJob> = initial
        private set
    override suspend fun load(): List<SyncJob> = jobs
    override suspend fun save(jobs: List<SyncJob>) { this.jobs = jobs }
}

/**
 * 단일 코루틴 워커 직렬 큐(§5.5). 로컬 저장 → [noteChanged] → 디바운스([SyncConfig.debounceMillis]) → Push 1건.
 * 실패한 작업은 큐 머리에 남겨 두고 다음 깨움(폴링·새 저장)에서 재시도한다.
 *
 * @param scope 워커·디바운스 코루틴이 사는 스코프(앱 수명, 테스트는 backgroundScope).
 */
class SerialSyncQueue(
    private val engine: SyncEngine,
    private val queueStore: QueueStore,
    private val config: SyncConfig,
    private val scope: CoroutineScope,
) {
    val status: StateFlow<SyncStatus> get() = engine.status

    private val lock = Mutex()
    private val pending = ArrayDeque<SyncJob>()
    private val dirtyNotes = LinkedHashSet<String>()
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private var debounceJob: Job? = null
    private var workerJob: Job? = null
    private var pollJob: Job? = null
    private var running: SyncJob? = null

    /** 영속화된 작업을 복원하고 워커를 시작한다. */
    suspend fun start() {
        lock.withLock {
            if (workerJob != null) return
            pending.clear()
            pending.addAll(queueStore.load())
            workerJob = scope.launch { workerLoop() }
        }
        if (pending.isNotEmpty()) wake.trySend(Unit)
    }

    /** 폴링 시작: [SyncConfig.pollIntervalMillis] 마다 Pull(§4.7). */
    fun startPolling() {
        if (pollJob != null) return
        pollJob = scope.launch {
            while (isActive) {
                delay(config.pollIntervalMillis)
                enqueue(SyncJob.Pull)
            }
        }
    }

    fun stop() {
        pollJob?.cancel(); pollJob = null
        debounceJob?.cancel(); debounceJob = null
        workerJob?.cancel(); workerJob = null
    }

    /** 로컬 저장 직후 호출(UI 스레드 가능). 디바운스 창이 열려 있으면 연장. */
    fun noteChanged(noteId: String) {
        scope.launch {
            lock.withLock {
                dirtyNotes += noteId
                debounceJob?.cancel()
                debounceJob = scope.launch {
                    delay(config.debounceMillis)
                    flushDirty()
                }
            }
        }
    }

    /** 디바운스를 기다리지 않고 즉시 Push 를 큐에 넣는다(앱 백그라운드 진입 등). */
    suspend fun flushNow() {
        lock.withLock { debounceJob?.cancel(); debounceJob = null }
        flushDirty()
    }

    suspend fun enqueue(job: SyncJob) {
        lock.withLock {
            val last = pending.lastOrNull()
            when {
                job is SyncJob.Push && last is SyncJob.Push && last !== running -> {
                    pending.removeLast()
                    pending.addLast(mergePush(last, job))
                }
                job is SyncJob.Pull && last is SyncJob.Pull && last !== running -> Unit
                else -> pending.addLast(job)
            }
            queueStore.save(pending.toList())
        }
        wake.trySend(Unit)
    }

    /** 대기 중 작업 수(테스트·진단용). */
    suspend fun pendingCount(): Int = lock.withLock { pending.size }

    private suspend fun flushDirty() {
        val ids = lock.withLock { dirtyNotes.toSet().also { dirtyNotes.clear() } }
        if (ids.isEmpty()) return
        enqueue(SyncJob.Push(ids))
    }

    private fun mergePush(a: SyncJob.Push, b: SyncJob.Push): SyncJob.Push =
        if (a.noteIds.isEmpty() || b.noteIds.isEmpty()) SyncJob.Push() else SyncJob.Push(a.noteIds + b.noteIds)

    private suspend fun workerLoop() {
        while (scope.isActive) {
            wake.receive()
            while (true) {
                val job = lock.withLock { pending.firstOrNull()?.also { running = it } } ?: break
                val ok = try {
                    when (job) {
                        is SyncJob.Push -> engine.pushNotes(job.noteIds.ifEmpty { null })
                        SyncJob.Pull -> engine.pull()
                    }
                    true
                } catch (e: CancellationException) {
                    lock.withLock { running = null }
                    throw e
                } catch (_: Exception) {
                    false // 엔진이 status 를 ERROR/OFFLINE 으로 바꿨다. 작업은 남겨 두고 다음 깨움에서 재시도.
                }
                lock.withLock {
                    running = null
                    if (ok) {
                        pending.removeFirst()
                        queueStore.save(pending.toList())
                    }
                }
                if (!ok) break
            }
        }
    }
}

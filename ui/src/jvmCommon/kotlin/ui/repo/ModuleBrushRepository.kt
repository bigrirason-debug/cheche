package io.github.bigrirason_debug.platen.ui.repo

import io.github.bigrirason_debug.platen.brush.BrushImporter
import io.github.bigrirason_debug.platen.brush.BrushPackage
import io.github.bigrirason_debug.platen.brush.LoadedBrush
import io.github.bigrirason_debug.platen.model.BrushSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * brush 모듈 연결 로스터: 내장 9종 + 사용자 임포트(.brush/.brushset/.abr) + 노트 동봉 브러시.
 *
 * 획은 `spec.id`(안정 슬러그)로 브러시를 참조하고, 노트에는 그 id 를 키로 brush zip 을 동봉한다(자기완결, D-017).
 * 임포트한 브러시는 [storeDir] 에 `<id>.zip` 으로 보관해 재시작 후에도 남는다.
 */
class ModuleBrushRepository(private val storeDir: File?) : BrushRepository {
    private val loaded = LinkedHashMap<String, LoadedBrush>()
    override val brushes = MutableStateFlow<List<BrushSpec>>(emptyList())

    init {
        for (spec in BuiltinBrushes.all) loaded[spec.id] = LoadedBrush(spec, BuiltinBrushes.tipPng(spec.id), BuiltinBrushes.grainPng(spec.id))
        storeDir?.listFiles { f -> f.isFile && f.extension == "zip" }?.sortedBy { it.name }?.forEach { f ->
            runCatching { BrushPackage.load(f.readBytes()) }.getOrNull()?.let { put(it) }
        }
        publish()
    }

    @Synchronized private fun put(b: LoadedBrush) { loaded[b.spec.id] = b }
    @Synchronized private fun publish() { brushes.value = loaded.values.map { it.spec } }

    override fun get(id: String): BrushSpec? = synchronized(this) { loaded[id]?.spec }

    override suspend fun import(name: String, bytes: ByteArray): BrushImportResult = withContext(Dispatchers.Default) {
        val result = runCatching { BrushImporter.import(name, bytes) }
        val r = result.getOrElse { e -> return@withContext BrushImportResult(null, emptyList(), e.message ?: e.toString()) }
        if (r.brushes.isEmpty()) return@withContext BrushImportResult(null, r.notes, "브러시를 찾지 못했습니다")
        for (b in r.brushes) {
            // 같은 슬러그가 이미 있으면(내장과 충돌 포함) 접미사로 구분한다
            var spec = b.spec
            if (get(spec.id) != null && get(spec.id) != spec) spec = spec.copy(id = spec.id + "-" + (System.currentTimeMillis() % 100000))
            val fixed = LoadedBrush(spec, b.tipPng, b.grainPng)
            put(fixed)
            storeDir?.let { d -> d.mkdirs(); File(d, "${spec.id}.zip").writeBytes(BrushPackage.write(fixed)) }
        }
        publish()
        val first = r.brushes.first().spec
        val notes = r.notes + r.brushes.flatMap { it.spec.source.lossNotes }
        BrushImportResult(get(first.id) ?: brushes.value.last(), notes.distinct())
    }

    override fun zipBytes(id: String): ByteArray? = synchronized(this) { loaded[id] }?.let { BrushPackage.write(it) }

    override fun registerEmbedded(zips: Map<String, ByteArray>) {
        var changed = false
        for ((id, bytes) in zips) {
            if (get(id) != null) continue
            runCatching { BrushPackage.load(bytes) }.getOrNull()?.let { b ->
                put(if (b.spec.id == id) b else LoadedBrush(b.spec.copy(id = id), b.tipPng, b.grainPng)); changed = true
            }
        }
        if (changed) publish()
    }

    override fun tipPng(id: String): ByteArray? = synchronized(this) { loaded[id]?.tipPng }
    override fun grainPng(id: String): ByteArray? = synchronized(this) { loaded[id]?.grainPng }
}

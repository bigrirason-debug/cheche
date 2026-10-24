package io.github.bigrirason_debug.platen.model

import kotlinx.serialization.Serializable

/** 입력 샘플(문서 좌표 pt). t = 획 시작 기준 ms. 틸트는 라디안(x,y 성분), 없으면 0. */
@Serializable
data class InputSample(
    val x: Float,
    val y: Float,
    val pressure: Float,
    val tiltX: Float = 0f,
    val tiltY: Float = 0f,
    val t: Float = 0f,
)

/**
 * 획. 원시 샘플을 보존하고(손글씨 인식 후속 대비, D-012) 지오메트리는 렌더 시 파생한다.
 * color = ARGB(Int, 부호 있음). 다크 모드 재매핑은 렌더러 정책.
 */
@Serializable
data class Stroke(
    val id: String,
    val brushId: String,
    val color: Int,
    /** 100% 줌 기준 지름(pt) — 브러시 size.base 를 사용자 슬라이더가 덮어쓴 값 */
    val size: Float,
    val opacity: Float = 1f,
    val overrides: StrokeOverrides? = null,
    val samples: List<InputSample>,
    val createdAt: Long = 0L,
) {
    fun bounds(pad: Float = size): Rect = Rect.ofPoints(samples.map { Pt(it.x, it.y) }).inflate(pad)
}

@Serializable
sealed class Background {
    @Serializable data class Solid(val color: Int) : Background()
    /** 내장 속지: kind ∈ lines/grid/dots/blank, spacing pt, 마진 스톱(좌/우/상 여백 pt) */
    @Serializable data class Template(
        val kind: String,
        val spacing: Float = 24f,
        val color: Int = 0x33000000,
        val marginLeft: Float = 48f,
        val marginRight: Float = 48f,
        val marginTop: Float = 64f,
        val paper: Int = 0xFFF6F1E7.toInt(),
    ) : Background()
    @Serializable data class PdfPage(val assetId: String, val pageIndex: Int) : Background()
    @Serializable data class Image(val assetId: String) : Background()
}

@Serializable
sealed class PageObject {
    abstract val id: String
    abstract val rect: Rect

    @Serializable
    data class TextBox(
        override val id: String,
        override val rect: Rect,
        val text: String,
        val fontSize: Float = 14f,
        val color: Int = 0xFF1A1A1A.toInt(),
        val mono: Boolean = false,
    ) : PageObject()

    @Serializable
    data class Image(
        override val id: String,
        override val rect: Rect,
        val assetId: String,
        val rotationDeg: Float = 0f,
    ) : PageObject()

    /** 표: 행·열·선 스타일·셀 크기 조절(PLAN A7). cells 는 "r,c" → 텍스트 */
    @Serializable
    data class Table(
        override val id: String,
        override val rect: Rect,
        val rows: Int,
        val cols: Int,
        val colWidths: List<Float>,
        val rowHeights: List<Float>,
        val lineWidth: Float = 1f,
        val lineColor: Int = 0xFF1A1A1A.toInt(),
        val cells: Map<String, String> = emptyMap(),
    ) : PageObject()

    /** 하이퍼링크 영역(PDF 플래너 링크 보존용). target = "page:<pageId>" | "index:<n>" | URL */
    @Serializable
    data class Link(override val id: String, override val rect: Rect, val target: String) : PageObject()
}

@Serializable
data class Page(
    val id: String,
    val size: Size = PaperSizes.DEFAULT,
    val background: Background = Background.Template("lines"),
    val strokes: List<Stroke> = emptyList(),
    val objects: List<PageObject> = emptyList(),
)

@Serializable
data class AssetRef(val id: String, val mime: String, val bytes: Long)

/** 노트(메모리 모델). 컨테이너 직렬화는 PlatenContainer. */
@Serializable
data class Note(
    val id: String,
    val title: String,
    val created: Long,
    val modified: Long,
    val pages: List<Page>,
    val cover: String? = null,
    val tags: List<String> = emptyList(),
) {
    fun usedBrushIds(): Set<String> = pages.flatMap { p -> p.strokes.map { it.brushId } }.toSet()
    fun usedAssetIds(): Set<String> = buildSet {
        for (p in pages) {
            when (val b = p.background) {
                is Background.PdfPage -> add(b.assetId)
                is Background.Image -> add(b.assetId)
                else -> {}
            }
            p.objects.forEach { if (it is PageObject.Image) add(it.assetId) }
        }
    }
}

/** manifest.json */
@Serializable
data class NoteManifest(
    val schema: Int = PlatenSchema.WRITE_VERSION,
    val minReader: Int = PlatenSchema.MIN_READER,
    val generator: String = "platen",
    val noteId: String,
    val title: String,
    val created: Long,
    val modified: Long,
    val pageIds: List<String>,
    val brushes: List<String> = emptyList(),
    val assets: List<AssetRef> = emptyList(),
    val cover: String? = null,
    val tags: List<String> = emptyList(),
)

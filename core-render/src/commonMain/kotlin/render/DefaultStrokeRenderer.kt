package io.github.bigrirason_debug.platen.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode as ComposeBlend
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import io.github.bigrirason_debug.platen.ink.PathPoint
import io.github.bigrirason_debug.platen.ink.ResolvedInkParams
import io.github.bigrirason_debug.platen.ink.StrokeGeometry
import io.github.bigrirason_debug.platen.model.BrushEngine
import io.github.bigrirason_debug.platen.model.Stroke

/**
 * 기본 획 렌더러. core-ink 의 [StrokeGeometry] 출력(Outline/StampPlan)만 소비하며 플랫폼 분기 없이 DrawScope API 로 그린다.
 * - profile 엔진: smooth → outline → Path(nonZero fill) → drawPath. 캡 모양은 outline 에 이미 반영되어 있다(core-ink 책임).
 * - stamp 엔진: smooth → stampPlan → 스탬프마다 팁 이미지를 지름·각도·알파로 drawImage(8-bit alpha 팁을 획 색으로 틴트).
 * - 색/혼합: [InkColorPolicy] (다크 재매핑, multiply↔screen, erase→Clear).
 *
 * @param geometry 테스트에서 가짜 지오메트리를 주입할 수 있도록 생성자 인자로 둔다(core-ink 와 병행 구현).
 */
class DefaultStrokeRenderer(
    private val images: BrushImageStore,
    private val geometry: StrokeGeometry = StrokeGeometry.Default,
) : StrokeRenderer {

    override fun DrawScope.drawStroke(stroke: Stroke, params: ResolvedInkParams, theme: InkTheme) {
        val center = geometry.smooth(stroke.samples, params)
        if (center.isEmpty()) return
        val color = InkColorPolicy.remapInk(stroke.color, params.spec.role, theme)
        val blend = InkColorPolicy.blendModeFor(params.spec, theme)
        when (params.spec.engine) {
            BrushEngine.profile -> drawProfile(center, params, color, blend)
            // 시드 = 획 id 해시: 같은 획은 항상 같은 배치(결정적 렌더, 골든 테스트 전제)
            BrushEngine.stamp -> drawStamps(center, params, color, blend, seed = stroke.id.hashCode().toLong())
        }
    }

    override fun DrawScope.drawLive(points: List<PathPoint>, params: ResolvedInkParams, color: Int, theme: InkTheme) {
        if (points.isEmpty()) return
        val c = InkColorPolicy.remapInk(color, params.spec.role, theme)
        val blend = InkColorPolicy.blendModeFor(params.spec, theme)
        // 젖은 잉크는 stamp 엔진도 임시로 profile 경로(outline fill)로 그린다:
        // 매 프레임 stampPlan + 수백 drawImage 는 비용이 크고, 확정(drawStroke) 시 정확한 스탬프로 교체되므로 성능을 우선한다.
        drawProfile(points, params, c, blend)
    }

    private fun DrawScope.drawProfile(center: List<PathPoint>, params: ResolvedInkParams, color: Int, blend: ComposeBlend) {
        val outline = geometry.outline(center, params)
        val poly = outline.polygon
        if (poly.size < 6) return
        val path = Path().apply {
            fillType = PathFillType.NonZero
            moveTo(poly[0], poly[1])
            var i = 2
            while (i + 1 < poly.size) {
                lineTo(poly[i], poly[i + 1])
                i += 2
            }
            close()
        }
        val composeColor = Color(color)
        drawPath(path, color = composeColor, alpha = params.opacity.coerceIn(0f, 1f), blendMode = blend)
    }

    private fun DrawScope.drawStamps(center: List<PathPoint>, params: ResolvedInkParams, color: Int, blend: ComposeBlend, seed: Long) {
        val plan = geometry.stampPlan(center, params, seed)
        if (plan.stamps.isEmpty()) return
        val spec = params.spec
        val baseTip = images.tip(spec)
        val precomposed = (images as? PrecomposedTips)?.takeIf { images.grain(spec) != null }
        val composeColor = Color(color)
        val tint = ColorFilter.tint(composeColor, ComposeBlend.SrcIn)
        val opacity = params.opacity.coerceIn(0f, 1f)

        for (s in plan.stamps) {
            val alpha = (s.alpha * opacity).coerceIn(0f, 1f)
            if (alpha <= 0f || s.diameter <= 0f) continue
            val tip: ImageBitmap? = precomposed?.let { it.tipVariant(spec, s.variant.mod(it.variantCount)) } ?: baseTip
            if (tip == null) {
                // 팁 이미지가 없으면 원형 스탬프로 폴백(임포트 실패·내장 브러시 기본값)
                drawCircle(composeColor, radius = s.diameter / 2f, center = Offset(s.x, s.y), alpha = alpha, blendMode = blend)
                continue
            }
            val sx = s.diameter / tip.width
            val sy = s.diameter / tip.height
            withTransform({
                translate(s.x, s.y)
                rotate(degrees = s.angleRad * RAD_TO_DEG, pivot = Offset.Zero)
                scale(sx, sy, pivot = Offset.Zero)
            }) {
                drawImage(
                    image = tip,
                    topLeft = Offset(-tip.width / 2f, -tip.height / 2f),
                    alpha = alpha,
                    colorFilter = tint,
                    blendMode = blend,
                )
            }
        }
    }

    private companion object {
        const val RAD_TO_DEG = 57.29578f
    }
}

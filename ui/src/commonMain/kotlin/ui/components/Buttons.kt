@file:Suppress("DEPRECATION")

package io.github.bigrirason_debug.platen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import io.github.bigrirason_debug.platen.ui.theme.LocalUiDensity
import io.github.bigrirason_debug.platen.ui.theme.MonoTextStyle
import io.github.bigrirason_debug.platen.ui.theme.PlatenIcon
import io.github.bigrirason_debug.platen.ui.theme.PlatenSpacing
import io.github.bigrirason_debug.platen.ui.theme.PlatenTheme

/**
 * 모든 아이콘 버튼은 이 래퍼만 쓴다: contentDescription = icon.label(비어 있지 않음), 툴팁 = 라벨 + 단축키(모노).
 * 활성 = 아이콘 아래 2dp 빨간 밑줄(DESIGN §5), 비활성 = muted.
 */
@Composable
fun ToolIconButton(
    icon: PlatenIcon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    label: String = icon.label,
    badgeColor: Color? = null,
) {
    val c = PlatenTheme.colors
    val target = PlatenSpacing.target * LocalUiDensity.current
    PlatenTooltip(text = label, shortcut = icon.shortcut) {
        Box(
            modifier = modifier.size(target).semantics { stateDescription = if (selected) "선택됨" else "" },
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(target)) {
                Icon(icon.vector, contentDescription = label, tint = if (enabled) c.ink else c.muted, modifier = Modifier.size(PlatenSpacing.iconSize))
            }
            if (badgeColor != null) {
                Box(
                    Modifier.align(Alignment.TopEnd).offset(x = (-8).dp, y = 8.dp).size(6.dp).clip(CircleShape).background(badgeColor),
                )
            }
            if (selected) {
                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp).width(24.dp).height(2.dp).background(c.red))
            }
        }
    }
}

@Composable
fun PlatenTooltip(text: String, shortcut: String? = null, content: @Composable () -> Unit) {
    TooltipBox(
        // rememberTooltipPositionProvider() 는 m3 1.4+ 에만 있어 양쪽에 있는 API 를 쓴다(1.12 에서는 deprecated 경고만).
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text)
                    if (shortcut != null) {
                        Spacer(Modifier.width(PlatenSpacing.s))
                        Text(shortcut, style = MonoTextStyle)
                    }
                }
            }
        },
        state = rememberTooltipState(),
        content = content,
    )
}

/** 섹션 제목(설정·시트 공용). */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(top = PlatenSpacing.m, bottom = PlatenSpacing.xs)) {
        Text(text, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = PlatenTheme.colors.ink)
    }
}

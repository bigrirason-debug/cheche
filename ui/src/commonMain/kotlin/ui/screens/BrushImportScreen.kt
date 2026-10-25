package io.github.bigrirason_debug.platen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.bigrirason_debug.platen.model.InkSettings
import io.github.bigrirason_debug.platen.ui.AppEnvironment
import io.github.bigrirason_debug.platen.ui.components.SectionTitle
import io.github.bigrirason_debug.platen.ui.components.ToolIconButton
import io.github.bigrirason_debug.platen.ui.diag.OutlinedAction
import io.github.bigrirason_debug.platen.ui.repo.BrushImportResult
import io.github.bigrirason_debug.platen.ui.state.PenSlot
import io.github.bigrirason_debug.platen.ui.theme.MonoTextStyle
import io.github.bigrirason_debug.platen.ui.theme.PlatenIcons
import io.github.bigrirason_debug.platen.ui.theme.PlatenSpacing
import io.github.bigrirason_debug.platen.ui.theme.PlatenTheme
import kotlinx.coroutines.launch

/** 브러시 가져오기: 파일 선택 → BrushRepository.import → 미리보기 획 + lossNotes + 슬롯에 넣기. */
@Composable
fun BrushImportScreen(env: AppEnvironment, ink: InkSettings, onAssignToSlot: (String) -> Unit, onBack: () -> Unit) {
    val c = PlatenTheme.colors
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<BrushImportResult?>(null) }
    var fileName by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(c.chrome)) {
        Row(Modifier.fillMaxWidth().height(PlatenSpacing.toolbarHeight), verticalAlignment = Alignment.CenterVertically) {
            ToolIconButton(PlatenIcons.Back, onClick = onBack)
            Text("브러시 가져오기", style = MaterialTheme.typography.titleLarge, color = c.ink)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = PlatenSpacing.l)) {
            Text("Procreate(.brush/.brushset)·Photoshop(.abr) 파일을 내 브러시로 변환합니다. 사용자 소유 파일만.", color = c.mutedText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = PlatenSpacing.s))
            OutlinedAction(if (busy) "가져오는 중…" else "파일 선택", enabled = !busy) {
                scope.launch {
                    busy = true
                    val f = env.files.pickFile("브러시 파일 선택", listOf("brush", "brushset", "abr"))
                    if (f != null) { fileName = f.name; result = env.brushes.import(f.name, f.bytes) }
                    busy = false
                }
            }
            val r = result
            if (r != null) {
                SectionTitle(fileName ?: "결과")
                if (r.error != null) Text("실패: ${r.error}", color = c.red)
                val spec = r.spec
                if (spec != null) {
                    Text("${spec.name} · ${spec.engine} · ${spec.source.format}", style = MonoTextStyle, color = c.ink)
                    StrokePreview(spec, PenSlot(spec.id, size = spec.size.base), ink, env.strokeRenderer, Modifier.fillMaxWidth().height(72.dp).padding(vertical = PlatenSpacing.s))
                    Row { OutlinedAction("선택 슬롯에 넣기") { onAssignToSlot(spec.id) } }
                }
                SectionTitle("손실 메모 (lossNotes)")
                if (r.lossNotes.isEmpty()) Text("손실 없음", color = c.mutedText)
                for (n in r.lossNotes) Row { Text("• ", color = c.mutedText); Text(n, color = c.ink, style = MaterialTheme.typography.bodyMedium) }
            }
            Spacer(Modifier.height(PlatenSpacing.xxl))
        }
    }
}

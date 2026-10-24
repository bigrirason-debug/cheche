package io.github.bigrirason_debug.platen.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 아이콘 1개 = 벡터 + 접근성 이름 + (선택) 단축키. 라벨이 비어 있으면 테스트가 실패한다(DESIGN §6).
 * 모든 아이콘은 자체 선화(24dp, 1.8px 라운드 스트로크) — 타사 에셋 없음. "선화 장식 금지"는 UI 크롬 그림에 대한 규칙이고
 * 기능 아이콘은 Material 관례대로 선 아이콘을 쓴다.
 */
data class PlatenIcon(val name: String, val label: String, val vector: ImageVector, val shortcut: String? = null)

private fun stroke(name: String, fill: Boolean = false, body: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .apply {
            if (fill) path(fill = SolidColor(Color.Black), pathFillType = PathFillType.NonZero, pathBuilder = body)
            else path(
                stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathBuilder = body,
            )
        }.build()

object PlatenIcons {
    val Pen = PlatenIcon("pen", "펜", stroke("pen") {
        moveTo(4f, 20f); lineTo(8f, 19f); lineTo(19f, 8f); lineTo(16f, 5f); lineTo(5f, 16f); close()
        moveTo(13f, 8f); lineTo(16f, 11f)
    }, "1")
    val Fountain = PlatenIcon("fountain", "만년필", stroke("fountain") {
        moveTo(5f, 19f); lineTo(14f, 10f); lineTo(17f, 13f); lineTo(8f, 22f); close()
        moveTo(14f, 10f); lineTo(19f, 5f); lineTo(21f, 7f); lineTo(17f, 13f)
        moveTo(10f, 17f); lineTo(12f, 15f)
    }, "2")
    val Pencil = PlatenIcon("pencil", "연필", stroke("pencil") {
        moveTo(3f, 21f); lineTo(6f, 15f); lineTo(17f, 4f); lineTo(20f, 7f); lineTo(9f, 18f); close()
        moveTo(6f, 15f); lineTo(9f, 18f); moveTo(15f, 6f); lineTo(18f, 9f)
    }, "3")
    val Highlighter = PlatenIcon("highlighter", "형광펜", stroke("highlighter") {
        moveTo(9f, 15f); lineTo(16f, 8f); lineTo(19f, 11f); lineTo(12f, 18f); lineTo(8f, 18f); close()
        moveTo(4f, 21f); lineTo(20f, 21f)
    }, "4")
    val Monoline = PlatenIcon("monoline", "모노라인", stroke("monoline") {
        moveTo(4f, 16f); curveTo(8f, 6f, 12f, 20f, 20f, 8f)
    }, "5")
    val Eraser = PlatenIcon("eraser", "지우개", stroke("eraser") {
        moveTo(7f, 20f); lineTo(3f, 16f); lineTo(14f, 5f); lineTo(20f, 11f); lineTo(11f, 20f); close()
        moveTo(8f, 10f); lineTo(14f, 16f); moveTo(11f, 20f); lineTo(20f, 20f)
    }, "E")
    val Lasso = PlatenIcon("lasso", "올가미", stroke("lasso") {
        moveTo(12f, 4f); curveTo(17f, 4f, 20f, 7f, 20f, 10f); curveTo(20f, 13f, 16f, 15f, 12f, 15f)
        curveTo(8f, 15f, 4f, 13f, 4f, 10f); curveTo(4f, 7f, 7f, 4f, 12f, 4f); close()
        moveTo(9f, 15f); curveTo(7f, 18f, 9f, 21f, 7f, 22f)
    }, "L")
    val Text = PlatenIcon("text", "텍스트 상자", stroke("text") {
        moveTo(5f, 5f); lineTo(19f, 5f); moveTo(12f, 5f); lineTo(12f, 19f); moveTo(9f, 19f); lineTo(15f, 19f)
    }, "T")
    val Image = PlatenIcon("image", "이미지 삽입", stroke("image") {
        moveTo(4f, 5f); lineTo(20f, 5f); lineTo(20f, 19f); lineTo(4f, 19f); close()
        moveTo(4f, 16f); lineTo(9f, 11f); lineTo(13f, 15f); lineTo(16f, 12f); lineTo(20f, 16f)
        moveTo(15f, 9f); lineTo(15.5f, 9f)
    }, "I")
    val Table = PlatenIcon("table", "표 삽입", stroke("table") {
        moveTo(4f, 5f); lineTo(20f, 5f); lineTo(20f, 19f); lineTo(4f, 19f); close()
        moveTo(4f, 10f); lineTo(20f, 10f); moveTo(4f, 14.5f); lineTo(20f, 14.5f); moveTo(10f, 5f); lineTo(10f, 19f)
    }, "B")
    val Laser = PlatenIcon("laser", "레이저 포인터", stroke("laser") {
        moveTo(12f, 3f); lineTo(12f, 6f); moveTo(12f, 18f); lineTo(12f, 21f); moveTo(3f, 12f); lineTo(6f, 12f); moveTo(18f, 12f); lineTo(21f, 12f)
        moveTo(12f, 9.5f); curveTo(13.4f, 9.5f, 14.5f, 10.6f, 14.5f, 12f); curveTo(14.5f, 13.4f, 13.4f, 14.5f, 12f, 14.5f)
        curveTo(10.6f, 14.5f, 9.5f, 13.4f, 9.5f, 12f); curveTo(9.5f, 10.6f, 10.6f, 9.5f, 12f, 9.5f); close()
    }, "R")
    val Undo = PlatenIcon("undo", "실행 취소", stroke("undo") {
        moveTo(8f, 7f); lineTo(4f, 11f); lineTo(8f, 15f); moveTo(4f, 11f); lineTo(15f, 11f); curveTo(18f, 11f, 20f, 13f, 20f, 15.5f); curveTo(20f, 18f, 18f, 20f, 15f, 20f); lineTo(10f, 20f)
    }, "Ctrl+Z")
    val Redo = PlatenIcon("redo", "다시 실행", stroke("redo") {
        moveTo(16f, 7f); lineTo(20f, 11f); lineTo(16f, 15f); moveTo(20f, 11f); lineTo(9f, 11f); curveTo(6f, 11f, 4f, 13f, 4f, 15.5f); curveTo(4f, 18f, 6f, 20f, 9f, 20f); lineTo(14f, 20f)
    }, "Ctrl+Y")
    val AddPage = PlatenIcon("addPage", "페이지 추가", stroke("addPage") {
        moveTo(6f, 3f); lineTo(14f, 3f); lineTo(19f, 8f); lineTo(19f, 21f); lineTo(6f, 21f); close()
        moveTo(14f, 3f); lineTo(14f, 8f); lineTo(19f, 8f); moveTo(12.5f, 12f); lineTo(12.5f, 18f); moveTo(9.5f, 15f); lineTo(15.5f, 15f)
    })
    val More = PlatenIcon("more", "더보기", stroke("more", fill = true) {
        for (cy in listOf(6f, 12f, 18f)) {
            moveTo(12f, cy - 1.6f); curveTo(12.9f, cy - 1.6f, 13.6f, cy - 0.9f, 13.6f, cy); curveTo(13.6f, cy + 0.9f, 12.9f, cy + 1.6f, 12f, cy + 1.6f)
            curveTo(11.1f, cy + 1.6f, 10.4f, cy + 0.9f, 10.4f, cy); curveTo(10.4f, cy - 0.9f, 11.1f, cy - 1.6f, 12f, cy - 1.6f); close()
        }
    })
    val Back = PlatenIcon("back", "뒤로", stroke("back") { moveTo(14f, 6f); lineTo(8f, 12f); lineTo(14f, 18f) }, "Esc")
    val Settings = PlatenIcon("settings", "설정", stroke("settings") {
        moveTo(4f, 7f); lineTo(20f, 7f); moveTo(4f, 12f); lineTo(20f, 12f); moveTo(4f, 17f); lineTo(20f, 17f)
        moveTo(9f, 5f); lineTo(9f, 9f); moveTo(15f, 10f); lineTo(15f, 14f); moveTo(7f, 15f); lineTo(7f, 19f)
    })
    val Search = PlatenIcon("search", "검색", stroke("search") {
        moveTo(10.5f, 4f); curveTo(14.1f, 4f, 17f, 6.9f, 17f, 10.5f); curveTo(17f, 14.1f, 14.1f, 17f, 10.5f, 17f)
        curveTo(6.9f, 17f, 4f, 14.1f, 4f, 10.5f); curveTo(4f, 6.9f, 6.9f, 4f, 10.5f, 4f); close(); moveTo(15.5f, 15.5f); lineTo(20f, 20f)
    })
    val Add = PlatenIcon("add", "새 노트", stroke("add") { moveTo(12f, 5f); lineTo(12f, 19f); moveTo(5f, 12f); lineTo(19f, 12f) }, "Ctrl+N")
    val Delete = PlatenIcon("delete", "삭제", stroke("delete") {
        moveTo(5f, 7f); lineTo(19f, 7f); moveTo(9f, 7f); lineTo(9f, 4f); lineTo(15f, 4f); lineTo(15f, 7f)
        moveTo(7f, 7f); lineTo(8f, 20f); lineTo(16f, 20f); lineTo(17f, 7f); moveTo(10.5f, 11f); lineTo(10.5f, 17f); moveTo(13.5f, 11f); lineTo(13.5f, 17f)
    }, "Del")
    val Close = PlatenIcon("close", "닫기", stroke("close") { moveTo(6f, 6f); lineTo(18f, 18f); moveTo(18f, 6f); lineTo(6f, 18f) }, "Esc")
    val Check = PlatenIcon("check", "확인", stroke("check") { moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 7f) })
    val ChevronLeft = PlatenIcon("chevronLeft", "이전 페이지", stroke("chevronLeft") { moveTo(14f, 7f); lineTo(9f, 12f); lineTo(14f, 17f) }, "PgUp")
    val ChevronRight = PlatenIcon("chevronRight", "다음 페이지", stroke("chevronRight") { moveTo(10f, 7f); lineTo(15f, 12f); lineTo(10f, 17f) }, "PgDn")
    /** 2색 리본: 위 검정 / 아래 빨강 — 색은 렌더 시 tint 와 별도로 그린다(RibbonToggleButton). */
    val Ribbon = PlatenIcon("ribbon", "검정/빨강 리본 전환", stroke("ribbon") {
        moveTo(4f, 8f); lineTo(20f, 8f); moveTo(4f, 16f); lineTo(20f, 16f); moveTo(4f, 5f); lineTo(4f, 19f); moveTo(20f, 5f); lineTo(20f, 19f)
    }, "X")
    val Import = PlatenIcon("import", "브러시 가져오기", stroke("import") {
        moveTo(12f, 4f); lineTo(12f, 15f); moveTo(8f, 11f); lineTo(12f, 15f); lineTo(16f, 11f); moveTo(5f, 19f); lineTo(19f, 19f)
    })
    val Export = PlatenIcon("export", "내보내기", stroke("export") {
        moveTo(12f, 15f); lineTo(12f, 4f); moveTo(8f, 8f); lineTo(12f, 4f); lineTo(16f, 8f); moveTo(5f, 19f); lineTo(19f, 19f)
    })
    val Sync = PlatenIcon("sync", "동기화", stroke("sync") {
        moveTo(19f, 10f); curveTo(18f, 6.5f, 15f, 4.5f, 12f, 4.5f); curveTo(8.5f, 4.5f, 6f, 7f, 5f, 10f); moveTo(5f, 14f); curveTo(6f, 17.5f, 9f, 19.5f, 12f, 19.5f)
        curveTo(15.5f, 19.5f, 18f, 17f, 19f, 14f); moveTo(16f, 10f); lineTo(19f, 10f); lineTo(19f, 7f); moveTo(8f, 14f); lineTo(5f, 14f); lineTo(5f, 17f)
    })
    val Diagnostics = PlatenIcon("diagnostics", "진단", stroke("diagnostics") {
        moveTo(3f, 12f); lineTo(7f, 12f); lineTo(9.5f, 6f); lineTo(13f, 18f); lineTo(15.5f, 12f); lineTo(21f, 12f)
    })
    val Paper = PlatenIcon("paper", "속지 편집", stroke("paper") {
        moveTo(5f, 3f); lineTo(19f, 3f); lineTo(19f, 21f); lineTo(5f, 21f); close()
        moveTo(8f, 9f); lineTo(16f, 9f); moveTo(8f, 13f); lineTo(16f, 13f); moveTo(8f, 17f); lineTo(13f, 17f)
    })
    val Open = PlatenIcon("open", "열기", stroke("open") {
        moveTo(3f, 7f); lineTo(9f, 7f); lineTo(11f, 9f); lineTo(21f, 9f); lineTo(21f, 19f); lineTo(3f, 19f); close()
    }, "Ctrl+O")
    val Save = PlatenIcon("save", "저장", stroke("save") {
        moveTo(4f, 4f); lineTo(17f, 4f); lineTo(20f, 7f); lineTo(20f, 20f); lineTo(4f, 20f); close()
        moveTo(8f, 4f); lineTo(8f, 9f); lineTo(15f, 9f); lineTo(15f, 4f); moveTo(7f, 20f); lineTo(7f, 14f); lineTo(17f, 14f); lineTo(17f, 20f)
    }, "Ctrl+S")
    val Account = PlatenIcon("account", "계정", stroke("account") {
        moveTo(12f, 4f); curveTo(14.2f, 4f, 16f, 5.8f, 16f, 8f); curveTo(16f, 10.2f, 14.2f, 12f, 12f, 12f); curveTo(9.8f, 12f, 8f, 10.2f, 8f, 8f); curveTo(8f, 5.8f, 9.8f, 4f, 12f, 4f); close()
        moveTo(4f, 20f); curveTo(4f, 16f, 7.5f, 14.5f, 12f, 14.5f); curveTo(16.5f, 14.5f, 20f, 16f, 20f, 20f)
    })
    val ZoomIn = PlatenIcon("zoomIn", "확대", stroke("zoomIn") { moveTo(12f, 6f); lineTo(12f, 18f); moveTo(6f, 12f); lineTo(18f, 12f) }, "Ctrl++")
    val ZoomOut = PlatenIcon("zoomOut", "축소", stroke("zoomOut") { moveTo(6f, 12f); lineTo(18f, 12f) }, "Ctrl+-")
    val RowAdd = PlatenIcon("rowAdd", "행 추가", stroke("rowAdd") { moveTo(4f, 8f); lineTo(20f, 8f); moveTo(4f, 13f); lineTo(20f, 13f); moveTo(12f, 15f); lineTo(12f, 21f); moveTo(9f, 18f); lineTo(15f, 18f) })
    val RowRemove = PlatenIcon("rowRemove", "행 삭제", stroke("rowRemove") { moveTo(4f, 8f); lineTo(20f, 8f); moveTo(4f, 13f); lineTo(20f, 13f); moveTo(9f, 18f); lineTo(15f, 18f) })
    val ColAdd = PlatenIcon("colAdd", "열 추가", stroke("colAdd") { moveTo(8f, 4f); lineTo(8f, 20f); moveTo(13f, 4f); lineTo(13f, 20f); moveTo(18f, 9f); lineTo(18f, 15f); moveTo(15f, 12f); lineTo(21f, 12f) })
    val ColRemove = PlatenIcon("colRemove", "열 삭제", stroke("colRemove") { moveTo(8f, 4f); lineTo(8f, 20f); moveTo(13f, 4f); lineTo(13f, 20f); moveTo(15f, 12f); lineTo(21f, 12f) })
    val Duplicate = PlatenIcon("duplicate", "복제", stroke("duplicate") {
        moveTo(8f, 8f); lineTo(20f, 8f); lineTo(20f, 20f); lineTo(8f, 20f); close(); moveTo(16f, 8f); lineTo(16f, 4f); lineTo(4f, 4f); lineTo(4f, 16f); lineTo(8f, 16f)
    }, "Ctrl+D")
    val Home = PlatenIcon("home", "노트북 서랍", stroke("home") {
        moveTo(4f, 6f); lineTo(20f, 6f); lineTo(20f, 20f); lineTo(4f, 20f); close(); moveTo(4f, 13f); lineTo(20f, 13f); moveTo(10f, 9.5f); lineTo(14f, 9.5f); moveTo(10f, 16.5f); lineTo(14f, 16.5f)
    })
    val Palm = PlatenIcon("palm", "손가락 필기", stroke("palm") {
        moveTo(8f, 12f); lineTo(8f, 5f); moveTo(11f, 11f); lineTo(11f, 3.5f); moveTo(14f, 11f); lineTo(14f, 4.5f); moveTo(17f, 12f); lineTo(17f, 7f)
        moveTo(8f, 12f); lineTo(8f, 15f); curveTo(8f, 18.5f, 10.5f, 21f, 13f, 21f); curveTo(15.5f, 21f, 17f, 19f, 17f, 16f); lineTo(17f, 12f); moveTo(8f, 13f); lineTo(5.5f, 10.5f)
    })

    /** 접근성 테스트용 전수 목록. 새 아이콘은 여기에 반드시 추가한다. */
    val all: List<PlatenIcon> = listOf(
        Pen, Fountain, Pencil, Highlighter, Monoline, Eraser, Lasso, Text, Image, Table, Laser, Undo, Redo, AddPage, More, Back,
        Settings, Search, Add, Delete, Close, Check, ChevronLeft, ChevronRight, Ribbon, Import, Export, Sync, Diagnostics, Paper,
        Open, Save, Account, ZoomIn, ZoomOut, RowAdd, RowRemove, ColAdd, ColRemove, Duplicate, Home, Palm,
    )
}

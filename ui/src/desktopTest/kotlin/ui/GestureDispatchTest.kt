package io.github.bigrirason_debug.platen.ui

import io.github.bigrirason_debug.platen.model.GestureAction
import io.github.bigrirason_debug.platen.model.GestureInput
import io.github.bigrirason_debug.platen.model.GestureMap
import io.github.bigrirason_debug.platen.ui.input.FingerGestureRecognizer
import io.github.bigrirason_debug.platen.ui.input.GestureActions
import io.github.bigrirason_debug.platen.ui.input.GestureDispatcher
import io.github.bigrirason_debug.platen.ui.input.InputPolicy
import io.github.bigrirason_debug.platen.ui.input.PointerRole
import io.github.bigrirason_debug.platen.ui.state.Tool
import io.github.bigrirason_debug.platen.ui.theme.PlatenPalette
import androidx.compose.ui.input.pointer.PointerType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GestureDispatchTest {
    private class Recorder : GestureActions {
        val log = ArrayList<String>()
        override fun undo() { log += "undo" }
        override fun redo() { log += "redo" }
        override fun selectEraser() { log += "eraser" }
        override fun selectLasso() { log += "lasso" }
        override fun previousTool() { log += "prevTool" }
        override fun nextPage() { log += "nextPage" }
        override fun previousPage() { log += "prevPage" }
        override fun selectLaser() { log += "laser" }
        override fun toggleFingerDraw() { log += "finger" }
        override fun ribbonToggle() { log += "ribbon" }
    }

    @Test
    fun defaultMapDispatchesPreviousAppConventions() {
        val r = Recorder()
        val d = GestureDispatcher(GestureMap(), r)
        assertEquals(GestureAction.undo, d.dispatch(GestureInput.twoFingerTap))
        assertEquals(GestureAction.redo, d.dispatch(GestureInput.threeFingerTap))
        assertEquals(GestureAction.eraser, d.dispatch(GestureInput.stylusButtonDrag))
        assertEquals(GestureAction.nextPage, d.dispatch(GestureInput.twoFingerSwipeLeft))
        assertEquals(GestureAction.none, d.dispatch(GestureInput.threeFingerSwipeLeft))
        assertEquals(listOf("undo", "redo", "eraser", "nextPage"), r.log)
    }

    @Test
    fun remappedTableIsHonoured() {
        val r = Recorder()
        val map = GestureMap(GestureMap.DEFAULT + (GestureInput.twoFingerTap to GestureAction.colorBlackRed) + (GestureInput.stylusButtonClick to GestureAction.laser))
        val d = GestureDispatcher(map, r)
        d.dispatch(GestureInput.twoFingerTap); d.dispatch(GestureInput.stylusButtonClick)
        assertEquals(listOf("ribbon", "laser"), r.log)
    }

    @Test
    fun recognizerTapSwipeDoubleTap() {
        val rec = FingerGestureRecognizer(tapSlopPx = 10f, swipeMinPx = 50f)
        rec.begin(1, 100f, 100f, 0); rec.update(2, 100f, 100f)
        assertEquals(GestureInput.twoFingerTap, rec.end(100))
        // 더블탭(같은 손가락 수, 350ms 안)
        rec.begin(2, 100f, 100f, 200)
        assertEquals(GestureInput.twoFingerDoubleTap, rec.end(260))
        // 세 손가락 좌 스와이프
        rec.begin(3, 300f, 100f, 1000); rec.update(3, 200f, 110f)
        assertEquals(GestureInput.threeFingerSwipeLeft, rec.end(1200))
        // 두 손가락 우 스와이프
        rec.begin(2, 100f, 100f, 2000); rec.update(2, 220f, 90f)
        assertEquals(GestureInput.twoFingerSwipeRight, rec.end(2300))
        // 한 손가락은 제스처 아님, 느린 두 손가락 정지도 아님
        rec.begin(1, 0f, 0f, 3000); assertNull(rec.end(3050))
        rec.begin(2, 0f, 0f, 4000); assertNull(rec.end(4800))
        // 대각선 이동(수평 우세 아님)은 스와이프 아님
        rec.begin(2, 0f, 0f, 5000); rec.update(2, 60f, 60f); assertNull(rec.end(5100))
    }

    @Test
    fun inputPolicyRoles() {
        // 스타일러스 = 그리기
        assertEquals(PointerRole.Draw, InputPolicy.roleFor(PointerType.Stylus, Tool.Pen, false, true, false, false, 0))
        // 손가락 = 팬(기본), 손가락 필기 토글 시 그리기, 두 손가락이면 항상 팬
        assertEquals(PointerRole.Navigate, InputPolicy.roleFor(PointerType.Touch, Tool.Pen, false, true, false, false, 1))
        assertEquals(PointerRole.Draw, InputPolicy.roleFor(PointerType.Touch, Tool.Pen, true, true, false, false, 1))
        assertEquals(PointerRole.Navigate, InputPolicy.roleFor(PointerType.Touch, Tool.Pen, true, true, false, false, 2))
        // 펜 호버/접촉 중 손가락 무시(팜)
        assertEquals(PointerRole.Ignore, InputPolicy.roleFor(PointerType.Touch, Tool.Pen, true, true, true, false, 1))
        assertEquals(PointerRole.Draw, InputPolicy.roleFor(PointerType.Touch, Tool.Pen, true, false, true, false, 1))
        // 마우스: 펜 도구면 팬, 올가미/텍스트 등은 도구 조작, mouseDraw 면 그리기
        assertEquals(PointerRole.Navigate, InputPolicy.roleFor(PointerType.Mouse, Tool.Pen, false, true, false, false, 0))
        assertEquals(PointerRole.Draw, InputPolicy.roleFor(PointerType.Mouse, Tool.Lasso, false, true, false, false, 0))
        assertEquals(PointerRole.Draw, InputPolicy.roleFor(PointerType.Mouse, Tool.Pen, false, true, false, true, 0))
    }

    @Test
    fun ribbonToggleHasOneMeaning() {
        assertEquals(PlatenPalette.RED, PlatenPalette.ribbonToggle(PlatenPalette.BLACK))
        assertEquals(PlatenPalette.BLACK, PlatenPalette.ribbonToggle(PlatenPalette.RED))
        assertEquals(PlatenPalette.BLACK, PlatenPalette.ribbonToggle(PlatenPalette.BLUE)) // 다른 색이면 검정으로
    }
}

package io.github.bigrirason_debug.platen.ui.state

/**
 * 범용 undo/redo 스택. push 는 redo 스택을 비운다. limit 초과 시 가장 오래된 항목을 버린다(기본 200 — 획 단위라 메모리 상한).
 */
class UndoStack<T>(private val limit: Int = 200) {
    private val undo = ArrayDeque<T>()
    private val redo = ArrayDeque<T>()

    val canUndo: Boolean get() = undo.isNotEmpty()
    val canRedo: Boolean get() = redo.isNotEmpty()
    val undoDepth: Int get() = undo.size
    val redoDepth: Int get() = redo.size

    fun push(item: T) {
        undo.addLast(item)
        redo.clear()
        while (undo.size > limit) undo.removeFirst()
    }

    /** 되돌릴 항목을 꺼내 redo 로 옮긴다. 없으면 null. */
    fun popUndo(): T? = undo.removeLastOrNull()?.also { redo.addLast(it) }

    /** 다시 실행할 항목을 꺼내 undo 로 옮긴다. 없으면 null. */
    fun popRedo(): T? = redo.removeLastOrNull()?.also { undo.addLast(it) }

    fun clear() { undo.clear(); redo.clear() }
}

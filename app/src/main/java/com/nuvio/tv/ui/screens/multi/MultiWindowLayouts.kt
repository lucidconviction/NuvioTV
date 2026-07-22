package com.robbdeeze.nuviotv.ui.screens.multi

data class SlotPos(
    val index: Int,
    val row: Int,
    val col: Int,
    val rowSpan: Int = 1,
    val colSpan: Int = 1
)

enum class MultiWindowLayout(val label: String) {
    TWO_X_1("2×1"),
    ONE_X_2("1×2"),
    V2_STACK("2 vert"),
    ONE_PLUS_2("1+2"),
    TWO_PLUS_1("2+1"),
    THREE_VERT("3 vert"),
    TWO_X_2("2×2"),
    ONE_DASH_2_DASH_1("1-2-1"),
    ONE_PLUS_4("1+4"),
    FOUR_PLUS_1("4+1"),
    THREE_PLUS_2("3+2"),
    THREE_X_2("3×2"),
    ONE_DASH_2_DASH_2_DASH_1("1-2-2-1"),
    TWO_X_3("2×3"),
    ONE_DASH_3_DASH_3("1-3-3"),
    ONE_PLUS_6("1+6"),
    FOUR_X_2("4×2"),
    ONE_DASH_3_DASH_3_DASH_1("1-3-3-1"),
    TWO_X_4("2×4"),
    THREE_X_3("3×3")
}

fun getValidLayouts(count: Int): List<MultiWindowLayout> = when (count) {
    2 -> listOf(MultiWindowLayout.ONE_X_2, MultiWindowLayout.TWO_X_1, MultiWindowLayout.V2_STACK)
    3 -> listOf(MultiWindowLayout.ONE_PLUS_2, MultiWindowLayout.THREE_VERT, MultiWindowLayout.TWO_PLUS_1)
    4 -> listOf(MultiWindowLayout.TWO_X_2, MultiWindowLayout.ONE_DASH_2_DASH_1)
    5 -> listOf(MultiWindowLayout.ONE_PLUS_4, MultiWindowLayout.FOUR_PLUS_1, MultiWindowLayout.THREE_PLUS_2)
    6 -> listOf(MultiWindowLayout.THREE_X_2, MultiWindowLayout.ONE_DASH_2_DASH_2_DASH_1, MultiWindowLayout.TWO_X_3)
    7 -> listOf(MultiWindowLayout.ONE_DASH_3_DASH_3, MultiWindowLayout.ONE_PLUS_6)
    8 -> listOf(MultiWindowLayout.FOUR_X_2, MultiWindowLayout.ONE_DASH_3_DASH_3_DASH_1, MultiWindowLayout.TWO_X_4)
    9 -> listOf(MultiWindowLayout.THREE_X_3)
    else -> emptyList()
}

fun defaultLayout(count: Int): MultiWindowLayout? = getValidLayouts(count).firstOrNull()

fun resolveLayout(count: Int): MultiWindowLayout? {
    if (count == 0) return null
    val store = MultiWindowStore
    return if (store.layoutLocked && store.currentLayout != null) {
        store.currentLayout
    } else {
        defaultLayout(count)
    }
}

fun calculateSlots(count: Int, layout: MultiWindowLayout): List<SlotPos> {
    val positions = when (layout) {
        MultiWindowLayout.TWO_X_1 -> listOf(
            SlotPos(0, 0, 0, 1, 1), SlotPos(1, 1, 0, 1, 1)
        )
        MultiWindowLayout.ONE_X_2 -> listOf(
            SlotPos(0, 0, 0, 1, 1), SlotPos(1, 0, 1, 1, 1)
        )
        MultiWindowLayout.ONE_PLUS_2 -> listOf(
            SlotPos(0, 0, 0, 1, 2), SlotPos(1, 1, 0, 1, 1), SlotPos(2, 1, 1, 1, 1)
        )
        MultiWindowLayout.TWO_PLUS_1 -> listOf(
            SlotPos(0, 0, 0, 1, 1), SlotPos(1, 0, 1, 1, 1), SlotPos(2, 1, 0, 1, 2)
        )
        MultiWindowLayout.V2_STACK -> listOf(
            SlotPos(0, 0, 0, 2, 1), SlotPos(1, 1, 0, 2, 1)
        )
        MultiWindowLayout.THREE_VERT -> listOf(
            SlotPos(0, 0, 0, 1, 1), SlotPos(1, 1, 0, 1, 1), SlotPos(2, 2, 0, 1, 1)
        )
        MultiWindowLayout.TWO_X_2 -> listOf(
            SlotPos(0, 0, 0), SlotPos(1, 0, 1), SlotPos(2, 1, 0), SlotPos(3, 1, 1)
        )
        MultiWindowLayout.ONE_DASH_2_DASH_1 -> listOf(
            SlotPos(0, 0, 0, 1, 2), SlotPos(1, 1, 0, 1, 1), SlotPos(2, 1, 1, 1, 1), SlotPos(3, 2, 0, 1, 2)
        )
        MultiWindowLayout.ONE_PLUS_4 -> listOf(
            SlotPos(0, 0, 0, 1, 2), SlotPos(1, 1, 0, 1, 1), SlotPos(2, 1, 1, 1, 1),
            SlotPos(3, 2, 0, 1, 1), SlotPos(4, 2, 1, 1, 1)
        )
        MultiWindowLayout.FOUR_PLUS_1 -> listOf(
            SlotPos(0, 0, 0, 1, 1), SlotPos(1, 0, 1, 1, 1), SlotPos(2, 1, 0, 1, 1),
            SlotPos(3, 1, 1, 1, 1), SlotPos(4, 2, 0, 1, 2)
        )
        MultiWindowLayout.THREE_PLUS_2 -> listOf(
            SlotPos(0, 0, 0, 1, 1), SlotPos(1, 0, 1, 1, 1), SlotPos(2, 0, 2, 1, 1),
            SlotPos(3, 1, 0, 1, 1), SlotPos(4, 1, 1, 1, 2)
        )
        MultiWindowLayout.THREE_X_2 -> listOf(
            SlotPos(0, 0, 0), SlotPos(1, 0, 1), SlotPos(2, 1, 0),
            SlotPos(3, 1, 1), SlotPos(4, 2, 0), SlotPos(5, 2, 1)
        )
        MultiWindowLayout.ONE_DASH_2_DASH_2_DASH_1 -> listOf(
            SlotPos(0, 0, 0, 1, 2), SlotPos(1, 1, 0, 1, 1), SlotPos(2, 1, 1, 1, 1),
            SlotPos(3, 2, 0, 1, 1), SlotPos(4, 2, 1, 1, 1), SlotPos(5, 3, 0, 1, 2)
        )
        MultiWindowLayout.TWO_X_3 -> listOf(
            SlotPos(0, 0, 0), SlotPos(1, 0, 1), SlotPos(2, 0, 2),
            SlotPos(3, 1, 0), SlotPos(4, 1, 1), SlotPos(5, 1, 2)
        )
        MultiWindowLayout.ONE_DASH_3_DASH_3 -> listOf(
            SlotPos(0, 0, 0, 1, 3), SlotPos(1, 1, 0, 1, 1), SlotPos(2, 1, 1, 1, 1),
            SlotPos(3, 1, 2, 1, 1), SlotPos(4, 2, 0, 1, 1), SlotPos(5, 2, 1, 1, 1), SlotPos(6, 2, 2, 1, 1)
        )
        MultiWindowLayout.ONE_PLUS_6 -> listOf(
            SlotPos(0, 0, 0, 1, 2), SlotPos(1, 1, 0, 1, 1), SlotPos(2, 1, 1, 1, 1),
            SlotPos(3, 2, 0, 1, 1), SlotPos(4, 2, 1, 1, 1), SlotPos(5, 3, 0, 1, 1), SlotPos(6, 3, 1, 1, 1)
        )
        MultiWindowLayout.FOUR_X_2 -> listOf(
            SlotPos(0, 0, 0), SlotPos(1, 0, 1), SlotPos(2, 1, 0), SlotPos(3, 1, 1),
            SlotPos(4, 2, 0), SlotPos(5, 2, 1), SlotPos(6, 3, 0), SlotPos(7, 3, 1)
        )
        MultiWindowLayout.ONE_DASH_3_DASH_3_DASH_1 -> listOf(
            SlotPos(0, 0, 0, 1, 4), SlotPos(1, 1, 0, 1, 1), SlotPos(2, 1, 1, 1, 1), SlotPos(3, 1, 2, 1, 1),
            SlotPos(4, 2, 0, 1, 1), SlotPos(5, 2, 1, 1, 1), SlotPos(6, 2, 2, 1, 1), SlotPos(7, 3, 0, 1, 4)
        )
        MultiWindowLayout.TWO_X_4 -> listOf(
            SlotPos(0, 0, 0), SlotPos(1, 0, 1), SlotPos(2, 0, 2), SlotPos(3, 0, 3),
            SlotPos(4, 1, 0), SlotPos(5, 1, 1), SlotPos(6, 1, 2), SlotPos(7, 1, 3)
        )
        MultiWindowLayout.THREE_X_3 -> listOf(
            SlotPos(0, 0, 0), SlotPos(1, 0, 1), SlotPos(2, 0, 2),
            SlotPos(3, 1, 0), SlotPos(4, 1, 1), SlotPos(5, 1, 2),
            SlotPos(6, 2, 0), SlotPos(7, 2, 1), SlotPos(8, 2, 2)
        )
    }
    return positions.take(count)
}

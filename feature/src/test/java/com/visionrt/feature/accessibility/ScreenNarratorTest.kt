package com.visionrt.feature.accessibility

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenNarratorTest {

    @Test
    fun composeListsButtonsTopToBottomWithHeaders() {
        val result = ScreenNarrator.compose(
            headers = listOf("Home", "Assistance inactive"),
            buttonLabels = listOf("Start assistance", "Silence speech", "Settings"),
            fromTop = "From the top of the screen to the bottom:",
            buttonFmt = { index, label -> "Button $index: $label" },
        )
        assertEquals(
            "Home. Assistance inactive. " +
                "From the top of the screen to the bottom: " +
                "Button 1: Start assistance. Button 2: Silence speech. Button 3: Settings.",
            result,
        )
    }

    @Test
    fun composeWithoutButtonsOmitsFromTopSection() {
        val result = ScreenNarrator.compose(
            headers = listOf("Help."),
            buttonLabels = emptyList(),
            fromTop = "From the top:",
            buttonFmt = { index, label -> "Button $index: $label" },
        )
        assertEquals("Help.", result)
    }

    @Test
    fun composeSkipsBlankHeaders() {
        val result = ScreenNarrator.compose(
            headers = listOf("Settings", "   "),
            buttonLabels = listOf("Replay training"),
            fromTop = "From the top:",
            buttonFmt = { index, label -> "Button $index: $label" },
        )
        assertEquals(
            "Settings. From the top: Button 1: Replay training.",
            result,
        )
    }
}

package com.eva.ai.presentation.components

import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test

class PrimaryButtonStyleTest {
    @Test fun pinkActionColourKeepsDarkTextReadable() {
        fun linear(channel: Float): Double = if (channel <= 0.04045f) channel / 12.92
            else ((channel + 0.055) / 1.055).pow(2.4)
        fun luminance(colour: androidx.compose.ui.graphics.Color) =
            0.2126 * linear(colour.red) + 0.7152 * linear(colour.green) + 0.0722 * linear(colour.blue)
        val contrast = (luminance(EvaColors.Action) + 0.05) / (luminance(EvaColors.Ink) + 0.05)
        assertTrue("Button text needs at least 4.5:1 contrast; actual=$contrast", contrast >= 4.5)
        assertTrue("Action buttons must use Eva pink", EvaColors.Action == EvaColors.Pink)
    }
}

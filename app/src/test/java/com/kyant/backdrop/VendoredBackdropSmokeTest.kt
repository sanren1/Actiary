package com.kyant.backdrop

import androidx.compose.ui.unit.dp
import com.kyant.backdrop.shadow.Shadow
import com.kyant.backdrop.highlight.Highlight
import org.junit.Test
import org.junit.Assert.assertEquals

class VendoredBackdropSmokeTest {
    @Test
    fun shadowDefaults() {
        assertEquals(24f.dp, Shadow().radius)
        assertEquals(1f, Shadow().alpha)
    }

    @Test
    fun highlightDefaults() {
        assertEquals(0.5f.dp, Highlight().width)
        assertEquals(1f, Highlight().alpha)
    }
}

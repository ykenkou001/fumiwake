package dev.fumiwake.core

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaceholderTest {
    @Test
    fun `greeting returns fumiwake`() {
        assertEquals("fumiwake", Placeholder.greeting())
    }
}

package dev.fumiwake.core.music

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DurationTextsTest {

    @Test
    fun `plain durations`() {
        assertEquals("全音符", DurationTexts.of(1, 0))
        assertEquals("2分音符", DurationTexts.of(2, 0))
        assertEquals("4分音符", DurationTexts.of(4, 0))
        assertEquals("8分音符", DurationTexts.of(8, 0))
        assertEquals("16分音符", DurationTexts.of(16, 0))
        assertEquals("32分音符", DurationTexts.of(32, 0))
    }

    @Test
    fun `dotted durations`() {
        assertEquals("付点4分音符", DurationTexts.of(4, 1))
        assertEquals("付点8分音符", DurationTexts.of(8, 1))
        assertEquals("付点2分音符", DurationTexts.of(2, 1))
    }

    @Test
    fun `rests`() {
        assertEquals("4分休符", DurationTexts.of(4, 0, isRest = true))
        assertEquals("全休符", DurationTexts.of(1, 0, isRest = true))
    }

    @Test
    fun `dotted rests`() {
        assertEquals("付点4分休符", DurationTexts.of(4, 1, isRest = true))
    }

    @Test
    fun `unsupported duration value throws`() {
        assertFailsWith<IllegalArgumentException> { DurationTexts.of(3, 0) }
    }
}

package dev.fumiwake.core.music

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FretPositionsTest {

    @Test
    fun `standard tuning sounds the six open strings`() {
        // 1弦から6弦へ。ギタリストの数え方（1弦 = 最も細い弦 = 最高音）。
        val expected = listOf(64, 59, 55, 50, 45, 40)
        for ((index, midi) in expected.withIndex()) {
            assertEquals(midi, FretPositions.midiOf(string = index + 1, fret = 0))
        }
    }

    @Test
    fun `low position frets sound the expected pitches`() {
        // docs/PHASE2_GUITAR_PLAN.md §6.4 のローポジション参照表から代表点を取る。
        assertEquals(48, FretPositions.midiOf(string = 5, fret = 3)) // C3
        assertEquals(53, FretPositions.midiOf(string = 4, fret = 3)) // F3
        assertEquals(57, FretPositions.midiOf(string = 3, fret = 2)) // A3
        assertEquals(60, FretPositions.midiOf(string = 2, fret = 1)) // C4
        assertEquals(43, FretPositions.midiOf(string = 6, fret = 3)) // G2
    }

    @Test
    fun `a pitch reachable on two strings is found on both, lowest string first`() {
        // A2 は 6弦5フレットと 5弦開放。指板図が下から上へ描く順に合わせる。
        assertEquals(
            listOf(FretPosition(6, 5), FretPosition(5, 0)),
            FretPositions.positionsOf(45),
        )
    }

    @Test
    fun `maxFret bounds the search and includes its own boundary`() {
        assertEquals(listOf(FretPosition(5, 0)), FretPositions.positionsOf(45, maxFret = 4))
        assertTrue(FretPositions.positionsOf(76, maxFret = 12).contains(FretPosition(1, 12)))
        assertTrue(FretPositions.positionsOf(76, maxFret = 11).isEmpty())
    }

    @Test
    fun `a pitch below the lowest string exists nowhere`() {
        assertEquals(emptyList(), FretPositions.positionsOf(39)) // 6弦開放 E2 の半音下
    }

    @Test
    fun `every position found for a pitch sounds that pitch again`() {
        // 往復不変。positionsOf と midiOf がずれたら、指板図と正誤判定が食い違う。
        for (midi in 40..88) {
            for (position in FretPositions.positionsOf(midi, maxFret = 24)) {
                assertEquals(midi, FretPositions.midiOf(position.string, position.fret))
            }
        }
    }

    @Test
    fun `otherPositionsOf excludes the position itself`() {
        // 5弦開放の A2 は 6弦5フレットにもある。返るのは「他の場所」だけ。
        assertEquals(
            listOf(FretPosition(6, 5)),
            FretPositions.otherPositionsOf(FretPosition(5, 0), minFret = 0, maxFret = 12),
        )
    }

    @Test
    fun `otherPositionsOf respects the exercise's declared fret range`() {
        // ローポジションの譜例（0..3）で、ネックの上の方の運指まで提示してはいけない。
        assertEquals(
            emptyList(),
            FretPositions.otherPositionsOf(FretPosition(5, 0), minFret = 0, maxFret = 3),
        )
        assertEquals(
            listOf(FretPosition(6, 5)),
            FretPositions.otherPositionsOf(FretPosition(5, 0), minFret = 0, maxFret = 5),
        )
    }

    @Test
    fun `otherPositionsOf finds every alternative when the range is wide`() {
        // E4 (64) は 1弦開放 / 2弦5 / 3弦9 / 4弦14 ... 12フレットまでなら3箇所。
        val others = FretPositions.otherPositionsOf(FretPosition(1, 0), minFret = 0, maxFret = 12)
        assertEquals(listOf(FretPosition(3, 9), FretPosition(2, 5)), others)
    }

    @Test
    fun `an out-of-range string is rejected rather than silently wrapping`() {
        assertFailsWith<IllegalArgumentException> { FretPositions.midiOf(string = 7, fret = 0) }
        assertFailsWith<IllegalArgumentException> { FretPositions.midiOf(string = 0, fret = 0) }
        assertFailsWith<IllegalArgumentException> { FretPositions.midiOf(string = 1, fret = -1) }
    }

    @Test
    fun `a non-standard tuning shifts the positions it should`() {
        val dropD = Tuning(listOf(64, 59, 55, 50, 45, 38))
        assertEquals(38, FretPositions.midiOf(string = 6, fret = 0, tuning = dropD))
        assertEquals(
            listOf(FretPosition(6, 7), FretPosition(5, 0)),
            FretPositions.positionsOf(45, tuning = dropD),
        )
    }
}

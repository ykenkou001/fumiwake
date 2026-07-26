package dev.fumiwake.core.music

import kotlin.test.Test
import kotlin.test.assertEquals

class NoteNamesTest {

    @Test
    fun `middle C is C4`() {
        assertEquals("ド (C4)", NoteNames.fromMidi(60))
    }

    @Test
    fun `sharp above middle C`() {
        assertEquals("ド# (C#4)", NoteNames.fromMidi(61))
    }

    @Test
    fun `open low E string - E2`() {
        assertEquals("ミ (E2)", NoteNames.fromMidi(40))
    }

    @Test
    fun `open A string - A2`() {
        assertEquals("ラ (A2)", NoteNames.fromMidi(45))
    }

    @Test
    fun `open D string - D3`() {
        assertEquals("レ (D3)", NoteNames.fromMidi(50))
    }

    @Test
    fun `open G string - G3`() {
        assertEquals("ソ (G3)", NoteNames.fromMidi(55))
    }

    @Test
    fun `open B string - B3`() {
        assertEquals("シ (B3)", NoteNames.fromMidi(59))
    }

    @Test
    fun `open high E string - E4`() {
        assertEquals("ミ (E4)", NoteNames.fromMidi(64))
    }

    @Test
    fun `full chromatic scale within one octave from C4`() {
        val expected = listOf(
            "ド (C4)", "ド# (C#4)", "レ (D4)", "レ# (D#4)", "ミ (E4)", "ファ (F4)",
            "ファ# (F#4)", "ソ (G4)", "ソ# (G#4)", "ラ (A4)", "ラ# (A#4)", "シ (B4)",
        )
        val actual = (0..11).map { NoteNames.fromMidi(60 + it) }
        assertEquals(expected, actual)
    }

    @Test
    fun `octave boundary just below C4 is B3`() {
        assertEquals("シ (B3)", NoteNames.fromMidi(59))
    }

    @Test
    fun `octave boundary just above B4 is C5`() {
        assertEquals("ド (C5)", NoteNames.fromMidi(72))
    }

    @Test
    fun `scientific drops the katakana reading`() {
        assertEquals("C4", NoteNames.scientific(60))
        assertEquals("E2", NoteNames.scientific(40))
        assertEquals("A#3", NoteNames.scientific(58))
    }

    @Test
    fun `toMidi parses naturals and sharps`() {
        assertEquals(60, NoteNames.toMidi("C4"))
        assertEquals(40, NoteNames.toMidi("E2"))
        assertEquals(66, NoteNames.toMidi("F#4"))
    }

    @Test
    fun `toMidi treats enharmonic spellings as the same pitch`() {
        // コンテンツは文脈に合う表記で書ける。検証は MIDI で行うので結果は表記に依存しない。
        assertEquals(NoteNames.toMidi("A#3"), NoteNames.toMidi("Bb3"))
        assertEquals(NoteNames.toMidi("C#3"), NoteNames.toMidi("Db3"))
        assertEquals(58, NoteNames.toMidi("B♭3"))
    }

    @Test
    fun `toMidi distinguishes the note B from a flat sign`() {
        assertEquals(59, NoteNames.toMidi("B3"))
        assertEquals(58, NoteNames.toMidi("Bb3"))
    }

    @Test
    fun `toMidi round-trips every pitch on the fretboard`() {
        for (midi in 40..88) {
            assertEquals(midi, NoteNames.toMidi(NoteNames.scientific(midi)))
        }
    }

    @Test
    fun `toMidi rejects what is not pitch notation`() {
        // null を返す（例外にしない）ので、呼び出し側は検証エラーとして報告できる。
        for (bad in listOf("", "H4", "C", "4", "ド", "C#", "C4x", "C 4")) {
            assertEquals(null, NoteNames.toMidi(bad), "expected null for '$bad'")
        }
    }
}

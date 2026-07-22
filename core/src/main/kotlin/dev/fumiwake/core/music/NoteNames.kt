package dev.fumiwake.core.music

/**
 * MIDI note number -> display name conversion.
 *
 * Uses scientific pitch notation for the octave (MIDI 60 == C4), paired with a
 * katakana note-name reading, e.g. `fromMidi(60) == "ド (C4)"`, `fromMidi(61) == "ド# (C#4)"`.
 */
object NoteNames {

    private val KATAKANA_NAMES = listOf(
        "ド", "ド#", "レ", "レ#", "ミ", "ファ", "ファ#", "ソ", "ソ#", "ラ", "ラ#", "シ",
    )

    private val LETTER_NAMES = listOf(
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B",
    )

    fun fromMidi(midi: Int): String {
        val pitchClass = Math.floorMod(midi, 12)
        val octave = Math.floorDiv(midi, 12) - 1
        return "${KATAKANA_NAMES[pitchClass]} (${LETTER_NAMES[pitchClass]}$octave)"
    }
}

package dev.fumiwake.core.music

/**
 * MIDI note number <-> note name conversion.
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

    /** Semitones above C for each natural letter name. */
    private val LETTER_OFFSETS = mapOf(
        'C' to 0, 'D' to 2, 'E' to 4, 'F' to 5, 'G' to 7, 'A' to 9, 'B' to 11,
    )

    private val SCIENTIFIC_PATTERN = Regex("^([A-Ga-g])([#♯b♭]?)(-?\\d+)$")

    fun fromMidi(midi: Int): String {
        val pitchClass = Math.floorMod(midi, 12)
        return "${KATAKANA_NAMES[pitchClass]} (${scientific(midi)})"
    }

    /** Just the scientific part, e.g. `scientific(48) == "C3"`. Spelled with sharps. */
    fun scientific(midi: Int): String {
        val pitchClass = Math.floorMod(midi, 12)
        val octave = Math.floorDiv(midi, 12) - 1
        return "${LETTER_NAMES[pitchClass]}$octave"
    }

    /**
     * Parses scientific pitch notation back to a MIDI number, or null if [name] is not
     * valid notation.
     *
     * Accepts both accidentals — `"A#3"` and `"Bb3"` return the same number — so content
     * can spell a note the way the surrounding music reads it. Everything downstream
     * compares MIDI numbers, so the spelling never changes a verification result.
     */
    fun toMidi(name: String): Int? {
        val match = SCIENTIFIC_PATTERN.matchEntire(name.trim()) ?: return null
        val (letterText, accidentalText, octaveText) = match.destructured

        val base = LETTER_OFFSETS[letterText.single().uppercaseChar()] ?: return null
        val accidental = when (accidentalText) {
            "" -> 0
            "#", "♯" -> 1
            "b", "♭" -> -1
            else -> return null
        }
        val octave = octaveText.toIntOrNull() ?: return null

        return (octave + 1) * 12 + base + accidental
    }
}

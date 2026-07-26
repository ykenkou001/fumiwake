package dev.fumiwake.core.music

/**
 * A place to put a finger: [string] is 1-based with 1 = highest (see [Tuning]),
 * [fret] 0 = open.
 */
data class FretPosition(val string: Int, val fret: Int)

/**
 * Where a pitch lives on the fretboard (DESIGN.md §4.2).
 *
 * Kept in `:core`, Android-free and locally testable, because it is the single source of
 * truth for three things at once: the fretboard drawing, the verification that bundled
 * content sounds what it claims, and — from Phase 2-B — deciding whether a user's answer
 * to "where is this note?" is correct.
 *
 * That last one is why [positionsOf] exists rather than a hand-authored list of alternate
 * positions per exercise: the alternates are derivable from pitch and tuning, so deriving
 * them removes both the authoring cost and the chance of authoring them wrong
 * (DESIGN.md §8 Q2).
 */
object FretPositions {

    /** The pitch sounded by [fret] on [string]. */
    fun midiOf(string: Int, fret: Int, tuning: Tuning = Tuning.STANDARD): Int {
        require(fret >= 0) { "fret $fret must be >= 0" }
        return tuning.openMidi(string) + fret
    }

    /**
     * Every place [midi] can be played at or below [maxFret], ordered from the lowest
     * string upward (6th string first in standard tuning) — the same bottom-to-top order
     * a fretboard diagram is drawn in, so callers never need to re-sort.
     *
     * Empty when the pitch is out of reach of this tuning and fret span.
     */
    fun positionsOf(midi: Int, maxFret: Int = 12, tuning: Tuning = Tuning.STANDARD): List<FretPosition> {
        require(maxFret >= 0) { "maxFret $maxFret must be >= 0" }
        return (tuning.stringCount downTo 1).mapNotNull { string ->
            val fret = midi - tuning.openMidi(string)
            if (fret in 0..maxFret) FretPosition(string, fret) else null
        }
    }

    /**
     * The *other* places the pitch at [position] can be played, restricted to
     * [minFret]..[maxFret].
     *
     * This is what the fretboard diagram shows in a lighter shade, and what Phase 2-B
     * will accept as equally-correct answers to "where is this note?". Bounding it by the
     * exercise's own declared fret range matters: without the bound, an exercise about
     * the first three frets would offer positions halfway up the neck that it never
     * intended to teach.
     */
    fun otherPositionsOf(
        position: FretPosition,
        minFret: Int,
        maxFret: Int,
        tuning: Tuning = Tuning.STANDARD,
    ): List<FretPosition> {
        require(minFret in 0..maxFret) { "invalid fret range $minFret..$maxFret" }
        val midi = midiOf(position.string, position.fret, tuning)
        return positionsOf(midi, maxFret, tuning)
            .filter { it != position && it.fret >= minFret }
    }
}

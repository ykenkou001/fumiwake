package dev.fumiwake.core.music

/**
 * A guitar tuning: the MIDI pitch each open string sounds.
 *
 * **String numbering: 1 = the highest-sounding (thinnest) string.** This is the
 * convention guitarists use ("the 6th string" is the low E) and the one alphaTex uses in
 * `<fret>.<string>.<duration>`, so it is the convention the bundled content is authored
 * in and the one this module speaks.
 *
 * alphaTab's own [alphaTab.model.Note.string] property is numbered the *other* way
 * (1 = lowest-sounding). Converting between the two is `:app`'s job, in `ScoreLocator` —
 * see the note there. Nothing in `:core` should ever see alphaTab's numbering.
 */
data class Tuning(val stringMidi: List<Int>) {

    init {
        require(stringMidi.isNotEmpty()) { "tuning must have at least one string" }
    }

    val stringCount: Int get() = stringMidi.size

    /** MIDI pitch of the open [string] (1-based, 1 = highest). */
    fun openMidi(string: Int): Int {
        require(string in 1..stringCount) { "string $string is out of range 1..$stringCount" }
        return stringMidi[string - 1]
    }

    companion object {
        /** Regular tuning: E4 B3 G3 D3 A2 E2, from the 1st string down to the 6th. */
        val STANDARD = Tuning(listOf(64, 59, 55, 50, 45, 40))
    }
}

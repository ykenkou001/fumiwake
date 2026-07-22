package dev.fumiwake.app.score

import alphaTab.model.Beat
import alphaTab.model.Note
import dev.fumiwake.core.exercise.Exercise
import dev.fumiwake.core.exercise.FocusElement

/**
 * All alphaTab-model <-> [FocusElement] addressing lives here
 * (docs/IMPLEMENTATION_PLAN.md §5 "ScoreLocatorの責務"), so any real-device position
 * drift found during M2b/M3 device testing is fixable in this one place.
 *
 * Generalizes the hit-test logic proven working on-device in the M2a spike
 * ([dev.fumiwake.app.SpikeActivity.showNoteInfo]/`showRestInfo`):
 *  - barIndex  = beat.voice.bar.index (resets to 0 at the start of every bar)
 *  - beatIndex = beat.index (resets to 0 at the start of every bar, per the
 *    alphaTex/[Exercise.focusElements] convention verified in
 *    docs/IMPLEMENTATION_PLAN.md §1 and the bundled rhythm exercises)
 *  - noteIndex = the note's position within `beat.notes` (null = whole-beat focus,
 *    used for rests: a rest [Beat] has an empty `notes` list)
 */
object ScoreLocator {

    /** (barIndex, beatIndex, noteIndex) for a [Beat] or [Note], per the FocusElement convention. */
    data class Location(val barIndex: Int, val beatIndex: Int, val noteIndex: Int?)

    /** Location of a whole beat (e.g. a rest) — [Location.noteIndex] is always null. */
    fun locate(beat: Beat): Location = Location(
        barIndex = beat.voice.bar.index.toInt(),
        beatIndex = beat.index.toInt(),
        noteIndex = null,
    )

    /** Location of a single note, addressed by its position within its beat's note list. */
    fun locate(note: Note): Location {
        val beat = note.beat
        val notes = beat.notes
        var noteIndex: Int? = null
        for (i in 0 until notes.length.toInt()) {
            if (notes.get(i) === note) {
                noteIndex = i
                break
            }
        }
        return Location(
            barIndex = beat.voice.bar.index.toInt(),
            beatIndex = beat.index.toInt(),
            noteIndex = noteIndex,
        )
    }

    /** The exercise's [FocusElement] whose text applies to this exact note, if any. */
    fun findFocus(exercise: Exercise, note: Note): FocusElement? {
        val loc = locate(note)
        return exercise.focusElements.firstOrNull {
            it.barIndex == loc.barIndex && it.beatIndex == loc.beatIndex && it.noteIndex == loc.noteIndex
        }
    }

    /** The exercise's [FocusElement] whose text applies to this whole beat (typically a rest). */
    fun findFocus(exercise: Exercise, beat: Beat): FocusElement? {
        val loc = locate(beat)
        return exercise.focusElements.firstOrNull {
            it.barIndex == loc.barIndex && it.beatIndex == loc.beatIndex && it.noteIndex == null
        }
    }
}

package dev.fumiwake.core.exercise

import kotlinx.serialization.Serializable

/**
 * A single tappable location in an exercise's score that is an instance of the
 * exercise's concept (DESIGN.md §3.1).
 *
 * [noteIndex] is null when the whole beat is the focus (e.g. a rest).
 *
 * [expectPitch] is what this exercise *claims* sounds here, in scientific pitch notation
 * ("C3", "F#4", "Bb3"), and is the input to the notation-vs-fingering verification layer
 * (DESIGN.md §3.4). alphaTex derives pitch from the fingering, so pitch and fingering can
 * never disagree with each other — what can disagree is the fingering and the prose about
 * it: a `3.5` typed as `3.4` still parses, still sounds a clean single note, and leaves
 * the explanation calling F3 "ド". Only a declared claim makes that machine-checkable.
 *
 * **[expectPitch] is the sounding pitch, not the written one.** Guitar notation is written
 * an octave above what sounds (alphaTab reports `displayTranspositionPitch = -12`), so
 * open low E is E2 here and E3 on the staff. Sounding pitch is what maps one-to-one onto
 * fretboard positions, so both the check and the diagram use it with no conversion.
 */
@Serializable
data class FocusElement(
    val barIndex: Int,
    val beatIndex: Int,
    val noteIndex: Int? = null,
    val noteText: String,
    val expectPitch: String? = null,
)

/**
 * The span of frets an exercise means to stay inside, inclusive.
 *
 * Declared rather than inferred: it is an assertion ("this exercise only asks for frets
 * 0-3"), so a stray 7th fret is a verification failure instead of a silent widening. It
 * also bounds the fretboard diagram and the alternate positions derived for it.
 */
@Serializable
data class FretRange(val min: Int, val max: Int)

/**
 * A single bundled exercise: one alphaTex score body isolating exactly one
 * concept, plus its explanation and tap-to-explain metadata.
 *
 * Kotlin counterpart of the Pydantic schema in DESIGN.md §3.1.
 */
@Serializable
data class Exercise(
    val exerciseId: String,
    val conceptId: String,
    val prerequisites: List<String> = emptyList(),
    val titleJa: String,
    val alphatex: String,
    val explanationJa: String,
    val focusElements: List<FocusElement> = emptyList(),
    val difficulty: Int,
    val variantOf: String? = null,
    val fretRange: FretRange? = null,
)

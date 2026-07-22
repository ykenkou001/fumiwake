package dev.fumiwake.core.exercise

import kotlinx.serialization.Serializable

/**
 * A single tappable location in an exercise's score that is an instance of the
 * exercise's concept (DESIGN.md §3.1).
 *
 * [noteIndex] is null when the whole beat is the focus (e.g. a rest).
 */
@Serializable
data class FocusElement(
    val barIndex: Int,
    val beatIndex: Int,
    val noteIndex: Int? = null,
    val noteText: String,
)

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
)

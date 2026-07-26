package dev.fumiwake.core.concept

import kotlinx.serialization.Serializable

/**
 * A single node in the notation-concept taxonomy (DESIGN.md §2.1).
 *
 * [id] follows the `<domain>.<name>` convention (e.g. "rhythm.tie") and is
 * validated by [ConceptGraph].
 *
 * [assumedKnown] marks a concept the target user is assumed to already know
 * (DESIGN.md §2.3). Such a concept carries no exercises, so the "all exercises
 * complete" promotion in [dev.fumiwake.core.progress.ProgressEvaluator] can never
 * reach it — without this flag, every concept depending on it stays LOCKED forever.
 */
@Serializable
data class Concept(
    val id: String,
    val nameJa: String,
    val dependsOn: List<String> = emptyList(),
    val assumedKnown: Boolean = false,
)

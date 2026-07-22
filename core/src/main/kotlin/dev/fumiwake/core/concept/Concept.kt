package dev.fumiwake.core.concept

import kotlinx.serialization.Serializable

/**
 * A single node in the notation-concept taxonomy (DESIGN.md §2.1).
 *
 * [id] follows the `<domain>.<name>` convention (e.g. "rhythm.tie") and is
 * validated by [ConceptGraph].
 */
@Serializable
data class Concept(
    val id: String,
    val nameJa: String,
    val dependsOn: List<String> = emptyList(),
)

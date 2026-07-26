package dev.fumiwake.core.progress

import dev.fumiwake.core.catalog.Catalog
import dev.fumiwake.core.concept.ConceptGraph

/**
 * Promotion logic for concept learning status. Kept entirely in `:core` so
 * `:app` only ever displays the state this returns (docs/IMPLEMENTATION_PLAN.md §4/§5).
 */
class ProgressEvaluator(
    private val graph: ConceptGraph,
    private val catalog: Catalog,
    private val store: ProgressStore,
) {

    /**
     * What counts as learned for every decision this class makes: what the user actually
     * completed, plus the concepts the taxonomy assumes known (DESIGN.md §2.3).
     *
     * Both call sites below must use this, not [ProgressStore.learnedConcepts] directly.
     * Feeding it only to the "is this concept itself learned?" check leaves the
     * *dependency satisfaction* check unaware of assumed-known concepts, which is the
     * half-fix that shows `pitch.staff-names` as known while everything depending on it
     * stays locked.
     */
    private fun effectiveLearned(): Set<String> = store.learnedConcepts() + graph.assumedKnownIds

    fun statusOf(conceptId: String): ConceptStatus {
        val learned = effectiveLearned()
        if (conceptId in learned) return ConceptStatus.LEARNED
        val concept = graph.get(conceptId) ?: return ConceptStatus.LOCKED
        return if (learned.containsAll(concept.dependsOn)) ConceptStatus.AVAILABLE else ConceptStatus.LOCKED
    }

    /**
     * Records completion of [exerciseId]. If every exercise of its concept is now
     * complete, promotes the concept to LEARNED. Returns the concept's status after
     * recording. Throws [IllegalArgumentException] for an unknown exerciseId.
     */
    fun recordCompletion(exerciseId: String): ConceptStatus {
        val exercise = catalog.byId(exerciseId)
            ?: throw IllegalArgumentException("unknown exerciseId: $exerciseId")

        store.markCompleted(exerciseId)

        val conceptExercises = catalog.byConcept(exercise.conceptId)
        val completed = store.completedExercises()
        if (conceptExercises.isNotEmpty() && conceptExercises.all { it.exerciseId in completed }) {
            store.markConceptLearned(exercise.conceptId)
        }

        return statusOf(exercise.conceptId)
    }
}

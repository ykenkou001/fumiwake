package dev.fumiwake.core.progress

/** Concept learning state, derived from progress data by [ProgressEvaluator]. */
enum class ConceptStatus { LOCKED, AVAILABLE, LEARNED }

/**
 * Persistence port for user progress. `:app` implements this with SQLite;
 * [InMemoryProgressStore] is the in-`:core` implementation used by tests.
 */
interface ProgressStore {
    fun completedExercises(): Set<String>
    fun markCompleted(exerciseId: String)
    fun learnedConcepts(): Set<String>
    fun markConceptLearned(conceptId: String)
}

/** Simple in-memory [ProgressStore], for tests (and any non-persistent use). */
class InMemoryProgressStore : ProgressStore {
    private val completed = mutableSetOf<String>()
    private val learned = mutableSetOf<String>()

    override fun completedExercises(): Set<String> = completed.toSet()

    override fun markCompleted(exerciseId: String) {
        completed.add(exerciseId)
    }

    override fun learnedConcepts(): Set<String> = learned.toSet()

    override fun markConceptLearned(conceptId: String) {
        learned.add(conceptId)
    }
}

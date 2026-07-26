package dev.fumiwake.core.progress

import dev.fumiwake.core.catalog.Catalog
import dev.fumiwake.core.concept.Concept
import dev.fumiwake.core.concept.ConceptGraph
import dev.fumiwake.core.exercise.Exercise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProgressEvaluatorTest {

    private fun exercise(exerciseId: String, conceptId: String, difficulty: Int = 1) = Exercise(
        exerciseId = exerciseId,
        conceptId = conceptId,
        titleJa = "title",
        alphatex = "4.3.4 |",
        explanationJa = "explanation",
        difficulty = difficulty,
    )

    private val graph = ConceptGraph(
        listOf(
            Concept("rhythm.beat", "拍と拍子"),
            Concept("rhythm.tie", "タイ", listOf("rhythm.beat")),
        )
    )

    private fun newEvaluator(exercises: List<Exercise>, store: ProgressStore = InMemoryProgressStore()) =
        ProgressEvaluator(graph, Catalog(graph, exercises), store) to store

    /** The assumed-known shape from DESIGN.md §2.3: a no-exercise prerequisite of a real concept. */
    private fun newAssumedKnownEvaluator(store: ProgressStore = InMemoryProgressStore()): Pair<ProgressEvaluator, ProgressStore> {
        val assumedGraph = ConceptGraph(
            listOf(
                Concept("pitch.staff-names", "五線上の音名", assumedKnown = true),
                Concept("guitar.open-strings", "開放弦", listOf("pitch.staff-names")),
            )
        )
        return ProgressEvaluator(assumedGraph, Catalog(assumedGraph, emptyList()), store) to store
    }

    @Test
    fun `a concept with no dependencies and nothing learned is AVAILABLE`() {
        val (evaluator, _) = newEvaluator(emptyList())
        assertEquals(ConceptStatus.AVAILABLE, evaluator.statusOf("rhythm.beat"))
    }

    @Test
    fun `a concept whose dependency is not yet learned is LOCKED`() {
        val (evaluator, _) = newEvaluator(emptyList())
        assertEquals(ConceptStatus.LOCKED, evaluator.statusOf("rhythm.tie"))
    }

    @Test
    fun `a concept becomes AVAILABLE once its dependency is learned`() {
        val store = InMemoryProgressStore()
        store.markConceptLearned("rhythm.beat")
        val (evaluator, _) = newEvaluator(emptyList(), store)
        assertEquals(ConceptStatus.AVAILABLE, evaluator.statusOf("rhythm.tie"))
    }

    @Test
    fun `a learned concept is LEARNED regardless of dependencies`() {
        val store = InMemoryProgressStore()
        store.markConceptLearned("rhythm.tie")
        val (evaluator, _) = newEvaluator(emptyList(), store)
        assertEquals(ConceptStatus.LEARNED, evaluator.statusOf("rhythm.tie"))
    }

    @Test
    fun `an assumedKnown concept is LEARNED with an empty progress store`() {
        val (evaluator, _) = newAssumedKnownEvaluator()
        assertEquals(ConceptStatus.LEARNED, evaluator.statusOf("pitch.staff-names"))
    }

    @Test
    fun `a concept depending only on an assumedKnown concept is AVAILABLE, not LOCKED`() {
        // DESIGN.md §2.3 の詰みの再現テスト。assumedKnown を statusOf の戻り値にだけ
        // 効かせて依存充足の判定に混ぜ忘れると、上のテストは通るのにこれが落ちる。
        val (evaluator, _) = newAssumedKnownEvaluator()
        assertEquals(ConceptStatus.AVAILABLE, evaluator.statusOf("guitar.open-strings"))
    }

    @Test
    fun `recordCompletion does not promote the concept until all its exercises are complete`() {
        val exercises = listOf(
            exercise("rhythm.beat.001", "rhythm.beat"),
            exercise("rhythm.beat.002", "rhythm.beat"),
        )
        val (evaluator, _) = newEvaluator(exercises)
        val status = evaluator.recordCompletion("rhythm.beat.001")
        assertEquals(ConceptStatus.AVAILABLE, status)
    }

    @Test
    fun `recordCompletion promotes the concept to LEARNED once all its exercises are complete`() {
        val exercises = listOf(
            exercise("rhythm.beat.001", "rhythm.beat"),
            exercise("rhythm.beat.002", "rhythm.beat"),
        )
        val (evaluator, store) = newEvaluator(exercises)
        evaluator.recordCompletion("rhythm.beat.001")
        val status = evaluator.recordCompletion("rhythm.beat.002")
        assertEquals(ConceptStatus.LEARNED, status)
        assertEquals(setOf("rhythm.beat"), store.learnedConcepts())
    }

    @Test
    fun `recordCompletion is idempotent`() {
        val exercises = listOf(exercise("rhythm.beat.001", "rhythm.beat"))
        val (evaluator, store) = newEvaluator(exercises)
        evaluator.recordCompletion("rhythm.beat.001")
        val statusAgain = evaluator.recordCompletion("rhythm.beat.001")
        assertEquals(ConceptStatus.LEARNED, statusAgain)
        assertEquals(setOf("rhythm.beat.001"), store.completedExercises())
        assertEquals(setOf("rhythm.beat"), store.learnedConcepts())
    }

    @Test
    fun `recordCompletion promotes a downstream concept from LOCKED to AVAILABLE via its dependency`() {
        val exercises = listOf(
            exercise("rhythm.beat.001", "rhythm.beat"),
        )
        val (evaluator, _) = newEvaluator(exercises)
        assertEquals(ConceptStatus.LOCKED, evaluator.statusOf("rhythm.tie"))
        evaluator.recordCompletion("rhythm.beat.001")
        assertEquals(ConceptStatus.AVAILABLE, evaluator.statusOf("rhythm.tie"))
    }

    @Test
    fun `recordCompletion throws for an unknown exerciseId`() {
        val (evaluator, _) = newEvaluator(emptyList())
        assertFailsWith<IllegalArgumentException> { evaluator.recordCompletion("rhythm.beat.999") }
    }
}

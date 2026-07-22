package dev.fumiwake.core.catalog

import dev.fumiwake.core.concept.Concept
import dev.fumiwake.core.concept.ConceptGraph
import dev.fumiwake.core.exercise.Exercise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CatalogTest {

    private fun exercise(
        exerciseId: String,
        conceptId: String,
        difficulty: Int,
    ) = Exercise(
        exerciseId = exerciseId,
        conceptId = conceptId,
        titleJa = "title $exerciseId",
        alphatex = "4.3.4 |",
        explanationJa = "explanation",
        difficulty = difficulty,
    )

    private val graph = ConceptGraph(
        listOf(
            Concept("rhythm.beat", "拍と拍子"),
            Concept("rhythm.tie", "タイ", listOf("rhythm.beat")),
            Concept("pitch.staff-names", "五線上の音名"),
            Concept("guitar.open-strings", "開放弦", listOf("pitch.staff-names")),
            Concept("structure.repeats", "反復記号", listOf("rhythm.beat")),
            Concept("harmony.chords", "コード記号"),
        )
    )

    @Test
    fun `byConcept orders by difficulty then exerciseId`() {
        val exercises = listOf(
            exercise("rhythm.tie.003", "rhythm.tie", 2),
            exercise("rhythm.tie.001", "rhythm.tie", 1),
            exercise("rhythm.tie.002", "rhythm.tie", 2),
        )
        val catalog = Catalog(graph, exercises)
        assertEquals(
            listOf("rhythm.tie.001", "rhythm.tie.002", "rhythm.tie.003"),
            catalog.byConcept("rhythm.tie").map { it.exerciseId },
        )
    }

    @Test
    fun `byConcept returns empty list for concept with no exercises`() {
        val catalog = Catalog(graph, emptyList())
        assertEquals(emptyList(), catalog.byConcept("rhythm.tie"))
    }

    @Test
    fun `byId finds an exercise or returns null`() {
        val exercises = listOf(exercise("rhythm.tie.001", "rhythm.tie", 1))
        val catalog = Catalog(graph, exercises)
        assertEquals("rhythm.tie.001", catalog.byId("rhythm.tie.001")?.exerciseId)
        assertNull(catalog.byId("rhythm.tie.999"))
    }

    @Test
    fun `domains only includes domains with at least one exercise`() {
        val exercises = listOf(
            exercise("rhythm.tie.001", "rhythm.tie", 1),
            exercise("harmony.chords.001", "harmony.chords", 1),
        )
        val catalog = Catalog(graph, exercises)
        assertEquals(listOf("rhythm", "harmony"), catalog.domains)
    }

    @Test
    fun `domains follow the fixed display order regardless of insertion order`() {
        val exercises = listOf(
            exercise("harmony.chords.001", "harmony.chords", 1),
            exercise("structure.repeats.001", "structure.repeats", 1),
            exercise("guitar.open-strings.001", "guitar.open-strings", 1),
            exercise("pitch.staff-names.001", "pitch.staff-names", 1),
            exercise("rhythm.tie.001", "rhythm.tie", 1),
        )
        val catalog = Catalog(graph, exercises)
        assertEquals(listOf("rhythm", "pitch", "guitar", "structure", "harmony"), catalog.domains)
    }

    @Test
    fun `domains is empty when there are no exercises`() {
        val catalog = Catalog(graph, emptyList())
        assertEquals(emptyList(), catalog.domains)
    }

    @Test
    fun `conceptsInDomain returns only that domain's concepts in topological order`() {
        val catalog = Catalog(graph, emptyList())
        val rhythmConcepts = catalog.conceptsInDomain("rhythm").map { it.id }
        assertEquals(listOf("rhythm.beat", "rhythm.tie"), rhythmConcepts)

        val guitarConcepts = catalog.conceptsInDomain("guitar").map { it.id }
        assertEquals(listOf("guitar.open-strings"), guitarConcepts)
    }

    @Test
    fun `conceptsInDomain returns empty list for a domain with no concepts`() {
        val catalog = Catalog(graph, emptyList())
        assertEquals(emptyList(), catalog.conceptsInDomain("nonexistent"))
    }
}

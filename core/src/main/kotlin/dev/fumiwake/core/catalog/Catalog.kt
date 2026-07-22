package dev.fumiwake.core.catalog

import dev.fumiwake.core.concept.Concept
import dev.fumiwake.core.concept.ConceptGraph
import dev.fumiwake.core.exercise.Exercise

/**
 * In-memory index of exercises over a [ConceptGraph] (DESIGN.md §9: SQLite is
 * reserved for user progress only; the bundled catalog is loaded straight from YAML).
 */
class Catalog(val graph: ConceptGraph, exercises: List<Exercise>) {

    private val allExercises: List<Exercise> = exercises.toList()

    private val byIdMap: Map<String, Exercise> = allExercises.associateBy { it.exerciseId }

    private val byConceptMap: Map<String, List<Exercise>> = allExercises
        .groupBy { it.conceptId }
        .mapValues { (_, list) -> list.sortedWith(compareBy({ it.difficulty }, { it.exerciseId })) }

    /** Exercises for [conceptId], ordered by difficulty (ties broken by exerciseId). */
    fun byConcept(conceptId: String): List<Exercise> = byConceptMap[conceptId] ?: emptyList()

    fun byId(exerciseId: String): Exercise? = byIdMap[exerciseId]

    /** Domains with at least one exercise, in the fixed display order. */
    val domains: List<String> by lazy {
        val present = allExercises.map { domainOf(it.conceptId) }.toSet()
        DOMAIN_ORDER.filter { it in present }
    }

    /** All concepts belonging to [domain], in topological order. */
    fun conceptsInDomain(domain: String): List<Concept> =
        graph.topologicalOrder().filter { domainOf(it.id) == domain }

    private fun domainOf(conceptId: String): String = conceptId.substringBefore('.')

    companion object {
        private val DOMAIN_ORDER = listOf("rhythm", "pitch", "guitar", "structure", "harmony")
    }
}

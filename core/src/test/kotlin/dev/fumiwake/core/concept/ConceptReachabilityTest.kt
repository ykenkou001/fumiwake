package dev.fumiwake.core.concept

import dev.fumiwake.core.exercise.ExerciseLoader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Gatekeeper for the dead-end that DESIGN.md §2.3 records: `ProgressEvaluator` only
 * promotes a concept to LEARNED once *every* exercise of that concept is complete, so a
 * concept with zero exercises can never be promoted, and everything depending on it stays
 * LOCKED forever. That is how the whole guitar domain became unreachable behind
 * `pitch.staff-names`.
 *
 * The invariant checked here:
 *
 * > A concept that has at least one exercise must have every one of its transitive
 * > prerequisites either carry exercises too, or be marked `assumedKnown`.
 *
 * Scoping it to concepts that *have* exercises is deliberate. Concepts with no content
 * yet (harmony, structure) are not dead ends — nothing sends the user there. Checking
 * every concept in the taxonomy would fail permanently on future-work domains and the
 * only way out would be to delete the test.
 */
class ConceptReachabilityTest {

    @Test
    fun `every concept that has exercises can actually be reached`() {
        val contentDir = System.getProperty("fumiwake.content.dir")
            ?: fail("system property 'fumiwake.content.dir' is not set")
        val root = File(contentDir)
        val yamlFiles = if (root.isDirectory) {
            root.walkTopDown().filter { it.isFile && it.extension == "yaml" }.toList()
        } else {
            emptyList()
        }
        if (yamlFiles.isEmpty()) {
            fail("no exercise YAML files found under '$contentDir'")
        }

        val graph = ConceptGraph.bundled()
        val conceptsWithExercises = yamlFiles
            .map { ExerciseLoader.fromYaml(it.readText()).conceptId }
            .toSet()

        val problems = mutableListOf<String>()
        for (conceptId in conceptsWithExercises.sorted()) {
            for (prerequisite in graph.prerequisitesOf(conceptId)) {
                if (prerequisite in conceptsWithExercises) continue
                if (prerequisite in graph.assumedKnownIds) continue
                problems += "$conceptId はエクササイズを持つが、前提の $prerequisite は " +
                    "エクササイズ 0 件かつ assumedKnown=false のため永久に LEARNED にならない " +
                    "（$conceptId は LOCKED のまま）"
            }
        }

        assertTrue(problems.isEmpty(), "到達不能な概念があります:\n${problems.joinToString("\n")}")
    }
}

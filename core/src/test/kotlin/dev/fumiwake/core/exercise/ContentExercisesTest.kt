package dev.fumiwake.core.exercise

import dev.fumiwake.core.concept.ConceptGraph
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.jupiter.api.Disabled

/**
 * Full-corpus validation of the bundled exercise YAML under
 * `content/exercises/` (system property `fumiwake.content.dir`, injected by
 * `core/build.gradle.kts`).
 *
 * Disabled at M1: `content/` is still empty (M1.5's job). Per
 * docs/IMPLEMENTATION_PLAN.md §6 the integration lead re-enables this class
 * once M1.5 lands real exercise files, at which point "zero files" becomes a
 * real failure rather than an expected M1-time state.
 */
@Disabled("enabled at M1.5 integration")
class ContentExercisesTest {

    @Test
    fun `every bundled exercise YAML parses and validates against the bundled taxonomy`() {
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
        val problems = mutableListOf<String>()

        for (file in yamlFiles) {
            try {
                val exercise = ExerciseLoader.fromYaml(file.readText())
                val errors = ExerciseLoader.validate(exercise, graph)
                if (errors.isNotEmpty()) {
                    problems += "${file.path}: ${errors.joinToString("; ")}"
                }
            } catch (e: ExerciseFormatException) {
                problems += "${file.path}: ${e.message}"
            }
        }

        assertTrue(problems.isEmpty(), "invalid exercise files:\n${problems.joinToString("\n")}")
    }
}

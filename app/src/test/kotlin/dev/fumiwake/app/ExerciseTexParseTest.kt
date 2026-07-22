package dev.fumiwake.app

import alphaTab.Settings
import alphaTab.importer.AlphaTexImporter
import dev.fumiwake.core.exercise.ExerciseLoader
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * CI gatekeeper (docs/IMPLEMENTATION_PLAN.md §6 M2b): every bundled exercise's `alphatex`
 * must actually parse with alphaTab's own [AlphaTexImporter]. `ExerciseLoader.validate`
 * (`:core`) deliberately does not check alphaTex syntax, since `:core` cannot depend on
 * alphaTab — this JVM unit test is the one place that closes that gap, running as part
 * of `:app:testDebugUnitTest` (a plain JVM test task; no emulator/SDK needed, confirmed
 * in docs/IMPLEMENTATION_PLAN.md §1).
 *
 * The test's working directory when run via Gradle is not guaranteed to be the module
 * directory, so both a `../content/exercises` (module-relative) and a `content/exercises`
 * (root-relative) candidate are tried.
 */
class ExerciseTexParseTest {

    @Test
    fun `every bundled exercise's alphatex parses via AlphaTexImporter`() {
        val contentDir = resolveContentDir()
        val yamlFiles = contentDir.walkTopDown().filter { it.isFile && it.extension == "yaml" }.toList()

        assertTrue("no exercise yaml files found under ${contentDir.path}", yamlFiles.isNotEmpty())

        val failures = mutableListOf<String>()
        for (file in yamlFiles) {
            try {
                val exercise = ExerciseLoader.fromYaml(file.readText())
                val importer = AlphaTexImporter()
                importer.initFromString(exercise.alphatex, Settings())
                val score = importer.readScore()
                if (score.masterBars.length.toInt() <= 0) {
                    failures += "${file.path}: parsed score has zero bars"
                }
            } catch (e: Exception) {
                failures += "${file.path}: ${e.message}"
            }
        }

        assertTrue("alphaTex parse failures:\n${failures.joinToString("\n")}", failures.isEmpty())
    }

    private fun resolveContentDir(): File {
        val candidates = listOf(File("../content/exercises"), File("content/exercises"))
        return candidates.firstOrNull { it.isDirectory }
            ?: throw AssertionError(
                "content/exercises not found relative to ${File(".").absolutePath} (tried $candidates)",
            )
    }
}

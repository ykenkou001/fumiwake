package dev.fumiwake.core.exercise

import com.charleskorn.kaml.Yaml
import dev.fumiwake.core.concept.ConceptGraph

/**
 * YAML (de)serialization and structural validation of [Exercise] documents.
 *
 * Error-handling split (docs/IMPLEMENTATION_PLAN.md §4): [fromYaml] throws
 * [ExerciseFormatException] on any parse failure; [validate] never throws and
 * instead returns a list of human-readable problems (empty = OK).
 */
object ExerciseLoader {

    private val EXERCISE_ID_PATTERN = Regex("^([a-z]+\\.[a-z0-9-]+)\\.(\\d{3})$")

    fun fromYaml(yaml: String): Exercise {
        return try {
            Yaml.default.decodeFromString(Exercise.serializer(), yaml)
        } catch (e: Exception) {
            throw ExerciseFormatException("failed to parse exercise YAML: ${e.message}", e)
        }
    }

    fun validate(e: Exercise, graph: ConceptGraph): List<String> {
        val errors = mutableListOf<String>()

        val idMatch = EXERCISE_ID_PATTERN.matchEntire(e.exerciseId)
        if (idMatch == null) {
            errors += "exerciseId '${e.exerciseId}' does not match required format '<conceptId>.NNN'"
        } else {
            val conceptPrefix = idMatch.groupValues[1]
            if (conceptPrefix != e.conceptId) {
                errors += "exerciseId '${e.exerciseId}' concept prefix '$conceptPrefix' does not match conceptId '${e.conceptId}'"
            }
        }

        if (graph.get(e.conceptId) == null) {
            errors += "conceptId '${e.conceptId}' is not present in the concept graph"
        } else {
            val allowedPrerequisites = graph.prerequisitesOf(e.conceptId)
            val invalidPrerequisites = e.prerequisites.filterNot { it in allowedPrerequisites }
            if (invalidPrerequisites.isNotEmpty()) {
                errors += "prerequisites $invalidPrerequisites are not a subset of the taxonomy's " +
                    "prerequisites for '${e.conceptId}' (allowed: $allowedPrerequisites)"
            }
        }

        if (e.difficulty !in 1..5) {
            errors += "difficulty ${e.difficulty} is out of range 1..5"
        }

        if (e.alphatex.isBlank()) {
            errors += "alphatex must not be blank"
        }

        e.focusElements.forEachIndexed { index, focusElement ->
            if (focusElement.barIndex < 0) {
                errors += "focusElements[$index].barIndex must be >= 0 (was ${focusElement.barIndex})"
            }
            if (focusElement.beatIndex < 0) {
                errors += "focusElements[$index].beatIndex must be >= 0 (was ${focusElement.beatIndex})"
            }
            val noteIndex = focusElement.noteIndex
            if (noteIndex != null && noteIndex < 0) {
                errors += "focusElements[$index].noteIndex must be null or >= 0 (was $noteIndex)"
            }
        }

        val variantOf = e.variantOf
        if (variantOf != null && !EXERCISE_ID_PATTERN.matches(variantOf)) {
            errors += "variantOf '$variantOf' does not match required exerciseId format"
        }

        return errors
    }
}

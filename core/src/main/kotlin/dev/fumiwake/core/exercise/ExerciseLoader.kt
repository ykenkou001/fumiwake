package dev.fumiwake.core.exercise

import com.charleskorn.kaml.Yaml
import dev.fumiwake.core.concept.ConceptGraph
import dev.fumiwake.core.music.NoteNames

/**
 * YAML (de)serialization and structural validation of [Exercise] documents.
 *
 * Error-handling split (docs/IMPLEMENTATION_PLAN.md §4): [fromYaml] throws
 * [ExerciseFormatException] on any parse failure; [validate] never throws and
 * instead returns a list of human-readable problems (empty = OK).
 */
object ExerciseLoader {

    private val EXERCISE_ID_PATTERN = Regex("^([a-z]+\\.[a-z0-9-]+)\\.(\\d{3})$")

    /**
     * Domains whose exercises are *about* which pitch is which, and so must declare
     * [FocusElement.expectPitch] rather than leaving it optional. Elsewhere (rhythm) the
     * pitch is incidental — every note is deliberately the same — and demanding a claim
     * would be noise.
     */
    private val PITCH_SUBJECT_DOMAINS = setOf("guitar", "pitch")

    private const val MAX_FRET = 24

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

        val domain = e.conceptId.substringBefore('.')
        val pitchIsTheSubject = domain in PITCH_SUBJECT_DOMAINS

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
            val expectPitch = focusElement.expectPitch
            when {
                expectPitch == null ->
                    // A whole-beat focus (noteIndex null) is a rest or a beat-level remark;
                    // there is no single pitch to claim, so the requirement does not apply.
                    if (pitchIsTheSubject && noteIndex != null) {
                        errors += "focusElements[$index].expectPitch is required for '$domain' exercises " +
                            "(pitch is the subject there, so every focus on a note must state the pitch it claims)"
                    }

                noteIndex == null ->
                    errors += "focusElements[$index].expectPitch is set but noteIndex is null; " +
                        "a pitch claim needs a specific note, not a whole beat"

                NoteNames.toMidi(expectPitch) == null ->
                    errors += "focusElements[$index].expectPitch '$expectPitch' is not scientific " +
                        "pitch notation (e.g. 'C3', 'F#4', 'Bb3')"
            }
        }

        val fretRange = e.fretRange
        if (fretRange == null) {
            if (domain == "guitar") {
                errors += "fretRange is required for 'guitar' exercises (it is the assertion that " +
                    "makes a stray out-of-position note a failure rather than a silent widening)"
            }
        } else {
            if (fretRange.min < 0 || fretRange.max > MAX_FRET) {
                errors += "fretRange ${fretRange.min}..${fretRange.max} is out of range 0..$MAX_FRET"
            }
            if (fretRange.min > fretRange.max) {
                errors += "fretRange min ${fretRange.min} is greater than max ${fretRange.max}"
            }
        }

        val variantOf = e.variantOf
        if (variantOf != null && !EXERCISE_ID_PATTERN.matches(variantOf)) {
            errors += "variantOf '$variantOf' does not match required exerciseId format"
        }

        return errors
    }
}

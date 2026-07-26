package dev.fumiwake.core.exercise

import dev.fumiwake.core.concept.Concept
import dev.fumiwake.core.concept.ConceptGraph
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ExerciseLoaderTest {

    private val graph = ConceptGraph(
        listOf(
            Concept("rhythm.beat", "拍と拍子"),
            Concept("rhythm.tie", "タイ", listOf("rhythm.beat")),
        )
    )

    private val validYaml = """
        exerciseId: rhythm.tie.001
        conceptId: rhythm.tie
        prerequisites:
          - rhythm.beat
        titleJa: タイの基本
        alphatex: "4.3.4 4.3.4 |"
        explanationJa: これはタイの説明です。
        focusElements:
          - barIndex: 0
            beatIndex: 0
            noteIndex: 0
            noteText: ここがタイの始点
        difficulty: 1
        variantOf: null
    """.trimIndent()

    @Test
    fun `fromYaml parses a well-formed document`() {
        val exercise = ExerciseLoader.fromYaml(validYaml)
        assertEquals("rhythm.tie.001", exercise.exerciseId)
        assertEquals("rhythm.tie", exercise.conceptId)
        assertEquals(listOf("rhythm.beat"), exercise.prerequisites)
        assertEquals("タイの基本", exercise.titleJa)
        assertEquals("4.3.4 4.3.4 |", exercise.alphatex)
        assertEquals("これはタイの説明です。", exercise.explanationJa)
        assertEquals(1, exercise.focusElements.size)
        assertEquals(0, exercise.focusElements[0].barIndex)
        assertEquals(0, exercise.focusElements[0].noteIndex)
        assertEquals(1, exercise.difficulty)
        assertEquals(null, exercise.variantOf)
    }

    @Test
    fun `fromYaml applies defaults for omitted optional fields`() {
        val yaml = """
            exerciseId: rhythm.beat.001
            conceptId: rhythm.beat
            titleJa: 拍の基本
            alphatex: "4.3.4 |"
            explanationJa: 拍とは何か。
            difficulty: 1
        """.trimIndent()
        val exercise = ExerciseLoader.fromYaml(yaml)
        assertEquals(emptyList(), exercise.prerequisites)
        assertEquals(emptyList(), exercise.focusElements)
        assertEquals(null, exercise.variantOf)
    }

    @Test
    fun `fromYaml wraps broken YAML in ExerciseFormatException`() {
        val brokenYaml = """
            exerciseId: rhythm.tie.001
            conceptId: [unterminated
        """.trimIndent()
        val ex = assertFailsWith<ExerciseFormatException> { ExerciseLoader.fromYaml(brokenYaml) }
        assertTrue(ex.message!!.isNotBlank())
    }

    @Test
    fun `fromYaml wraps document missing required fields`() {
        val yaml = """
            exerciseId: rhythm.tie.001
        """.trimIndent()
        val ex = assertFailsWith<ExerciseFormatException> { ExerciseLoader.fromYaml(yaml) }
        assertTrue(ex.message!!.isNotBlank())
    }

    // --- validate() ---

    private fun baseExercise(
        exerciseId: String = "rhythm.tie.001",
        conceptId: String = "rhythm.tie",
        prerequisites: List<String> = listOf("rhythm.beat"),
        difficulty: Int = 1,
        alphatex: String = "4.3.4 |",
        focusElements: List<FocusElement> = listOf(FocusElement(0, 0, 0, "note")),
        variantOf: String? = null,
    ) = Exercise(
        exerciseId = exerciseId,
        conceptId = conceptId,
        prerequisites = prerequisites,
        titleJa = "title",
        alphatex = alphatex,
        explanationJa = "explanation",
        focusElements = focusElements,
        difficulty = difficulty,
        variantOf = variantOf,
    )

    @Test
    fun `validate accepts a well-formed exercise`() {
        assertEquals(emptyList(), ExerciseLoader.validate(baseExercise(), graph))
    }

    @Test
    fun `validate rejects exerciseId not matching format`() {
        val errors = ExerciseLoader.validate(baseExercise(exerciseId = "rhythm-tie-001"), graph)
        assertTrue(errors.any { it.contains("exerciseId") })
    }

    @Test
    fun `validate rejects exerciseId whose concept prefix mismatches conceptId`() {
        val errors = ExerciseLoader.validate(
            baseExercise(exerciseId = "rhythm.beat.001", conceptId = "rhythm.tie"),
            graph,
        )
        assertTrue(errors.any { it.contains("does not match conceptId") })
    }

    @Test
    fun `validate rejects unknown conceptId`() {
        val errors = ExerciseLoader.validate(
            baseExercise(exerciseId = "rhythm.unknown.001", conceptId = "rhythm.unknown", prerequisites = emptyList()),
            graph,
        )
        assertTrue(errors.any { it.contains("not present in the concept graph") })
    }

    @Test
    fun `validate rejects prerequisites not a subset of taxonomy prerequisites`() {
        val errors = ExerciseLoader.validate(
            baseExercise(prerequisites = listOf("rhythm.beat", "rhythm.unrelated")),
            graph,
        )
        assertTrue(errors.any { it.contains("prerequisites") })
    }

    @Test
    fun `validate accepts empty prerequisites even though taxonomy allows more`() {
        assertEquals(emptyList(), ExerciseLoader.validate(baseExercise(prerequisites = emptyList()), graph))
    }

    @Test
    fun `validate rejects difficulty out of range - too low`() {
        val errors = ExerciseLoader.validate(baseExercise(difficulty = 0), graph)
        assertTrue(errors.any { it.contains("difficulty") })
    }

    @Test
    fun `validate rejects difficulty out of range - too high`() {
        val errors = ExerciseLoader.validate(baseExercise(difficulty = 6), graph)
        assertTrue(errors.any { it.contains("difficulty") })
    }

    @Test
    fun `validate accepts difficulty boundaries`() {
        assertEquals(emptyList(), ExerciseLoader.validate(baseExercise(difficulty = 1), graph))
        assertEquals(emptyList(), ExerciseLoader.validate(baseExercise(difficulty = 5), graph))
    }

    @Test
    fun `validate rejects blank alphatex`() {
        val errors = ExerciseLoader.validate(baseExercise(alphatex = "   "), graph)
        assertTrue(errors.any { it.contains("alphatex") })
    }

    @Test
    fun `validate rejects negative barIndex or beatIndex`() {
        val errors = ExerciseLoader.validate(
            baseExercise(focusElements = listOf(FocusElement(-1, -2, 0, "x"))),
            graph,
        )
        assertTrue(errors.any { it.contains("barIndex") })
        assertTrue(errors.any { it.contains("beatIndex") })
    }

    @Test
    fun `validate rejects negative noteIndex but allows null`() {
        val errorsNegative = ExerciseLoader.validate(
            baseExercise(focusElements = listOf(FocusElement(0, 0, -1, "x"))),
            graph,
        )
        assertTrue(errorsNegative.any { it.contains("noteIndex") })

        val errorsNull = ExerciseLoader.validate(
            baseExercise(focusElements = listOf(FocusElement(0, 0, null, "x"))),
            graph,
        )
        assertEquals(emptyList(), errorsNull)
    }

    @Test
    fun `validate rejects malformed variantOf`() {
        val errors = ExerciseLoader.validate(baseExercise(variantOf = "not-an-exercise-id"), graph)
        assertTrue(errors.any { it.contains("variantOf") })
    }

    @Test
    fun `validate accepts well-formed variantOf`() {
        assertEquals(emptyList(), ExerciseLoader.validate(baseExercise(variantOf = "rhythm.tie.000"), graph))
    }

    @Test
    fun `validate accepts null variantOf`() {
        assertEquals(emptyList(), ExerciseLoader.validate(baseExercise(variantOf = null), graph))
    }

    // --- expectPitch / fretRange（DESIGN.md §3.4 第三層の入力） ---

    private val guitarGraph = ConceptGraph(
        listOf(
            Concept("pitch.staff-names", "五線上の音名", assumedKnown = true),
            Concept("guitar.open-strings", "開放弦", listOf("pitch.staff-names")),
            Concept("pitch.octaves", "オクターブ", listOf("pitch.staff-names")),
        )
    )

    private fun guitarExercise(
        conceptId: String = "guitar.open-strings",
        exerciseId: String = "guitar.open-strings.001",
        focusElements: List<FocusElement> = listOf(FocusElement(0, 0, 0, "note", expectPitch = "E2")),
        fretRange: FretRange? = FretRange(0, 0),
    ) = Exercise(
        exerciseId = exerciseId,
        conceptId = conceptId,
        prerequisites = listOf("pitch.staff-names"),
        titleJa = "title",
        alphatex = "0.6.4 |",
        explanationJa = "explanation",
        focusElements = focusElements,
        difficulty = 1,
        fretRange = fretRange,
    )

    @Test
    fun `validate accepts a well-formed guitar exercise`() {
        assertEquals(emptyList(), ExerciseLoader.validate(guitarExercise(), guitarGraph))
    }

    @Test
    fun `validate requires expectPitch where pitch is the subject`() {
        for (conceptId in listOf("guitar.open-strings", "pitch.octaves")) {
            val errors = ExerciseLoader.validate(
                guitarExercise(
                    conceptId = conceptId,
                    exerciseId = "$conceptId.001",
                    focusElements = listOf(FocusElement(0, 0, 0, "note")),
                ),
                guitarGraph,
            )
            assertTrue(errors.any { it.contains("expectPitch") }, "expected an error for $conceptId, got: $errors")
        }
    }

    @Test
    fun `validate leaves expectPitch optional where pitch is incidental`() {
        // rhythm は全音符を同じ音高にして書く。主張を強制しても意味がない。
        assertEquals(emptyList(), ExerciseLoader.validate(baseExercise(), graph))
    }

    @Test
    fun `validate rejects expectPitch that is not pitch notation`() {
        val errors = ExerciseLoader.validate(
            guitarExercise(focusElements = listOf(FocusElement(0, 0, 0, "note", expectPitch = "ミ"))),
            guitarGraph,
        )
        assertTrue(errors.any { it.contains("expectPitch") }, errors.toString())
    }

    @Test
    fun `validate requires fretRange on guitar exercises`() {
        val errors = ExerciseLoader.validate(guitarExercise(fretRange = null), guitarGraph)
        assertTrue(errors.any { it.contains("fretRange") }, errors.toString())
    }

    @Test
    fun `validate rejects an inverted or out-of-range fretRange`() {
        val inverted = ExerciseLoader.validate(guitarExercise(fretRange = FretRange(5, 2)), guitarGraph)
        assertTrue(inverted.any { it.contains("fretRange") }, inverted.toString())

        val tooHigh = ExerciseLoader.validate(guitarExercise(fretRange = FretRange(0, 25)), guitarGraph)
        assertTrue(tooHigh.any { it.contains("fretRange") }, tooHigh.toString())

        val negative = ExerciseLoader.validate(guitarExercise(fretRange = FretRange(-1, 4)), guitarGraph)
        assertTrue(negative.any { it.contains("fretRange") }, negative.toString())
    }

    @Test
    fun `validate rejects expectPitch on a whole-beat focus`() {
        val errors = ExerciseLoader.validate(
            guitarExercise(focusElements = listOf(FocusElement(0, 0, null, "x", expectPitch = "E2"))),
            guitarGraph,
        )
        assertTrue(errors.any { it.contains("expectPitch") }, errors.toString())
    }

    @Test
    fun `validate does not demand expectPitch for a whole-beat focus`() {
        // 休符など、単一の音高を持たない位置。主張しようがないので要求しない。
        assertEquals(
            emptyList(),
            ExerciseLoader.validate(
                guitarExercise(focusElements = listOf(FocusElement(0, 0, null, "休符"))),
                guitarGraph,
            ),
        )
    }

    @Test
    fun `validate accepts enharmonic spellings of expectPitch`() {
        for (spelling in listOf("A#2", "Bb2", "B♭2")) {
            val errors = ExerciseLoader.validate(
                guitarExercise(
                    focusElements = listOf(FocusElement(0, 0, 0, "note", expectPitch = spelling)),
                ),
                guitarGraph,
            )
            assertEquals(emptyList(), errors, "expected '$spelling' to be accepted")
        }
    }

    @Test
    fun `validate can return multiple errors at once`() {
        val errors = ExerciseLoader.validate(
            baseExercise(difficulty = 9, alphatex = "", exerciseId = "bad-id"),
            graph,
        )
        assertTrue(errors.size >= 3, "expected multiple errors, got: $errors")
    }
}

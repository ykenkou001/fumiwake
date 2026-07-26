package dev.fumiwake.app

import alphaTab.Settings
import alphaTab.importer.AlphaTexImporter
import alphaTab.model.Note
import alphaTab.model.Score
import dev.fumiwake.app.score.ScoreLocator
import dev.fumiwake.core.exercise.Exercise
import dev.fumiwake.core.exercise.ExerciseLoader
import dev.fumiwake.core.music.FretPositions
import dev.fumiwake.core.music.NoteNames
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The third verification layer (DESIGN.md §3.4): does each exercise *sound* what it
 * *says* it sounds?
 *
 * [ExerciseTexParseTest] proves the alphaTex parses and [ExerciseMidiSemanticsTest]
 * proves the resulting music is well-formed. Neither can catch the failure that matters
 * once pitch becomes the subject: a `3.5` mistyped as `3.4` parses, sounds one clean
 * note, keeps every bar the right length — and leaves the explanation calling F3 "ド".
 *
 * alphaTex derives pitch from the fingering, so pitch and fingering cannot contradict
 * each other; what can go wrong is the content's *claim* about them. So the content
 * states its claim (`FocusElement.expectPitch`, `Exercise.fretRange`) and this test
 * checks the claim against the score.
 *
 * The same checks exist in `tools/texcheck`, which is the loop used while authoring:
 * `:app` tests need an Android SDK and so only run in CI.
 */
class ExerciseFingeringTest {

    @Test
    fun `core's fretboard math agrees with alphaTab on every bundled note`() {
        // 指板図も、Phase 2-B の正誤判定も FretPositions を単一の出どころにする。
        // その計算が alphaTab の実音とずれていたら、その上の全部がずれる。
        val failures = mutableListOf<String>()

        forEachNote { file, _, note ->
            val position = ScoreLocator.fingeringOf(note)
            val expected = FretPositions.midiOf(position.string, position.fret)
            val actual = note.realValue.toInt()
            if (expected != actual) {
                failures += "${file.name}: ${position.string}弦${position.fret}フレットを " +
                    ":core は ${NoteNames.scientific(expected)} と計算しましたが、" +
                    "alphaTab は ${NoteNames.scientific(actual)} を鳴らします"
            }
        }

        assertTrue(
            failures.joinToString("\n", prefix = ":core の指板計算が alphaTab と一致しません:\n"),
            failures.isEmpty(),
        )
    }

    @Test
    fun `every declared expectPitch is the pitch that actually sounds`() {
        val failures = mutableListOf<String>()

        for (file in exerciseFiles()) {
            val exercise = ExerciseLoader.fromYaml(file.readText())
            val score = parse(exercise.alphatex)

            for (focus in exercise.focusElements) {
                val declared = focus.expectPitch ?: continue
                // 表記の妥当性そのものは :core の validate が弾く。ここは実音との照合に集中する。
                val expected = NoteNames.toMidi(declared) ?: continue
                val where = "bar=${focus.barIndex} beat=${focus.beatIndex} note=${focus.noteIndex}"

                val note = noteAt(score, focus.barIndex, focus.beatIndex, focus.noteIndex)
                if (note == null) {
                    failures += "${file.name}: focusElements が譜面に存在しない位置を指しています ($where)"
                    continue
                }

                val actual = note.realValue.toInt()
                if (actual != expected) {
                    val position = ScoreLocator.fingeringOf(note)
                    failures += "${file.name}: ${focus.barIndex + 1}小節目${focus.beatIndex + 1}拍目は " +
                        "$declared と書かれていますが、実際に鳴るのは ${NoteNames.scientific(actual)} です " +
                        "(${position.string}弦${position.fret}フレット)"
                }
            }
        }

        assertTrue(
            failures.joinToString("\n", prefix = "コンテンツが主張する音と実際に鳴る音が違います:\n"),
            failures.isEmpty(),
        )
    }

    @Test
    fun `every note stays inside the exercise's declared fretRange`() {
        val failures = mutableListOf<String>()

        forEachNote { file, exercise, note ->
            val range = exercise.fretRange ?: return@forEachNote
            val position = ScoreLocator.fingeringOf(note)
            if (position.fret < range.min || position.fret > range.max) {
                failures += "${file.name}: ${position.string}弦${position.fret}フレットは " +
                    "宣言した fretRange ${range.min}..${range.max} の外です"
            }
        }

        assertTrue(
            failures.joinToString("\n", prefix = "宣言したポジション範囲を逸脱しています:\n"),
            failures.isEmpty(),
        )
    }

    /** alphaTab のコレクションは `[]` ではなく `get(index)` で辿る（ScoreLocatorTest と同じ作法）。 */
    private fun forEachNote(action: (File, Exercise, Note) -> Unit) {
        for (file in exerciseFiles()) {
            val exercise = ExerciseLoader.fromYaml(file.readText())
            val bars = parse(exercise.alphatex).tracks.get(0).staves.get(0).bars
            for (barIndex in 0 until bars.length.toInt()) {
                val beats = bars.get(barIndex).voices.get(0).beats
                for (beatIndex in 0 until beats.length.toInt()) {
                    val notes = beats.get(beatIndex).notes
                    for (noteIndex in 0 until notes.length.toInt()) {
                        action(file, exercise, notes.get(noteIndex))
                    }
                }
            }
        }
    }

    private fun noteAt(score: Score, barIndex: Int, beatIndex: Int, noteIndex: Int?): Note? {
        if (noteIndex == null) return null
        val bars = score.tracks.get(0).staves.get(0).bars
        if (barIndex >= bars.length.toInt()) return null
        val beats = bars.get(barIndex).voices.get(0).beats
        if (beatIndex >= beats.length.toInt()) return null
        val notes = beats.get(beatIndex).notes
        if (noteIndex >= notes.length.toInt()) return null
        return notes.get(noteIndex)
    }

    private fun parse(alphatex: String): Score {
        val importer = AlphaTexImporter()
        importer.initFromString(alphatex, Settings())
        return importer.readScore()
    }

    private fun exerciseFiles(): List<File> {
        val candidates = listOf(File("../content/exercises"), File("content/exercises"))
        val dir = candidates.firstOrNull { it.isDirectory }
            ?: throw AssertionError("content/exercises not found relative to ${File(".").absolutePath}")
        val files = dir.walkTopDown().filter { it.isFile && it.extension == "yaml" }.toList()
        assertTrue("no exercise yaml files found under ${dir.path}", files.isNotEmpty())
        return files
    }
}

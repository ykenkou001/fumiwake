package dev.fumiwake.app

import alphaTab.Settings
import alphaTab.importer.AlphaTexImporter
import alphaTab.midi.ControllerType
import alphaTab.midi.IMidiFileHandler
import alphaTab.midi.MidiFileGenerator
import dev.fumiwake.core.exercise.ExerciseLoader
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * CI gatekeeper for the *meaning* of each exercise, not just its syntax
 * (docs/AUDIO_DEFECT_PLAN.md §4).
 *
 * [ExerciseTexParseTest] only proves that the alphaTex parses. That is not enough: the
 * "ビィィィーン" defect was written as `-.2`, which parses perfectly well — as a tie to
 * *string 2* rather than the intended "tie of duration 2". With no note on string 2 to
 * tie to, alphaTab sounded a phantom B3 that droned for eleven beats underneath the rest
 * of the exercise. Syntax was valid; the music was wrong.
 *
 * This test therefore runs alphaTab's own [MidiFileGenerator] — the same code path that
 * feeds the synthesizer on device — and asserts invariants that hold for every bundled
 * exercise:
 *
 *  1. **No overlapping notes.** All bundled exercises are single-voice, single-line
 *     (1譜例1概念; see docs/RHYTHM_COMPLETION_PLAN.md §3), so two notes sounding at once
 *     means something generated a note the score never asked for.
 *  2. **Every tie has a target.** A `-.<string>` token must name the same string as the
 *     preceding note, otherwise it starts a phantom note instead of extending one.
 *  3. **Every bar is exactly as long as its time signature.** Miscounted bars are
 *     otherwise invisible until played.
 */
class ExerciseMidiSemanticsTest {

    /** One generated MIDI note: when it starts, how long it lasts, which key it sounds. */
    private data class Note(val start: Double, val length: Double, val key: Int)

    /** Collects the notes [MidiFileGenerator] emits; every other MIDI event is irrelevant here. */
    private class NoteCollector : IMidiFileHandler {
        val notes = mutableListOf<Note>()

        override fun addNote(
            track: Double,
            start: Double,
            length: Double,
            key: Double,
            velocity: Double,
            channel: Double,
        ) {
            notes += Note(start, length, key.toInt())
        }

        override fun addTimeSignature(tick: Double, timeSignatureNumerator: Double, timeSignatureDenominator: Double) = Unit
        override fun addRest(track: Double, tick: Double, channel: Double) = Unit
        override fun addControlChange(track: Double, tick: Double, channel: Double, controller: ControllerType, value: Double) = Unit
        override fun addProgramChange(track: Double, tick: Double, channel: Double, program: Double) = Unit
        override fun addTempo(tick: Double, tempo: Double) = Unit
        override fun addNoteBend(track: Double, tick: Double, channel: Double, key: Double, value: Double) = Unit
        override fun addBend(track: Double, tick: Double, channel: Double, value: Double) = Unit
        override fun finishTrack(track: Double, tick: Double) = Unit
        override fun addTickShift(tick: Double) = Unit
    }

    @Test
    fun `no bundled exercise sounds a note the score never asked for`() {
        val failures = mutableListOf<String>()

        for (file in exerciseFiles()) {
            val alphatex = ExerciseLoader.fromYaml(file.readText()).alphatex
            val notes = generateNotes(alphatex).sortedBy { it.start }

            // 単旋律なので、ある音が鳴り終わる前に次の音が始まったら異常。
            for (i in notes.indices) {
                val a = notes[i]
                for (j in i + 1 until notes.size) {
                    val b = notes[j]
                    if (b.start >= a.start + a.length) break
                    failures += "${file.name}: 音が重なっています " +
                        "(key=${a.key} start=${a.start.toInt()} len=${a.length.toInt()} と " +
                        "key=${b.key} start=${b.start.toInt()})"
                }
            }
        }

        assertTrue(failures.joinToString("\n", prefix = "単旋律のはずが同時に鳴る音があります:\n"), failures.isEmpty())
    }

    @Test
    fun `every tie extends the preceding note instead of starting a phantom one`() {
        val failures = mutableListOf<String>()

        for (file in exerciseFiles()) {
            val alphatex = ExerciseLoader.fromYaml(file.readText()).alphatex
            var previousString: Int? = null

            for (token in NOTE_OR_TIE.findAll(alphatex)) {
                val noteString = token.groups[NOTE_STRING]?.value
                val tieString = token.groups[TIE_STRING]?.value

                if (noteString != null) {
                    previousString = noteString.toInt()
                    continue
                }

                // alphaTex のタイは `-.<弦>[.<音価>]`。音価ではなく弦を書く点に注意
                // （この取り違えが本テストを書くきっかけになった不具合）。
                val tie = tieString?.toInt() ?: continue
                if (tie != previousString) {
                    failures += "${file.name}: タイ `-.$tie` の相手がいません " +
                        "(直前の音は${previousString ?: "なし"}弦)。alphaTex のタイは `-.<弦>` です"
                }
            }
        }

        assertTrue(failures.joinToString("\n", prefix = "タイの指定が不正です:\n"), failures.isEmpty())
    }

    @Test
    fun `every bar lasts exactly as long as its time signature`() {
        val failures = mutableListOf<String>()

        for (file in exerciseFiles()) {
            val alphatex = ExerciseLoader.fromYaml(file.readText()).alphatex
            val importer = AlphaTexImporter()
            importer.initFromString(alphatex, Settings())
            val score = importer.readScore()

            // alphaTab のコレクションは Kotlin の [] 演算子ではなく get(index) で辿る
            // （ScoreLocatorTest と同じ作法）。
            val staves = score.tracks.get(0).staves
            for (staffIndex in 0 until staves.length.toInt()) {
                val bars = staves.get(staffIndex).bars
                for (barIndex in 0 until bars.length.toInt()) {
                    val expected = score.masterBars.get(barIndex).calculateDuration(true)
                    val actual = bars.get(barIndex).voices.get(0).calculateDuration()
                    if (actual != expected) {
                        failures += "${file.name}: ${barIndex + 1}小節目の長さが拍子と合いません " +
                            "(期待 ${expected.toInt()} ticks / 実際 ${actual.toInt()} ticks)"
                    }
                }
            }
        }

        assertTrue(failures.joinToString("\n", prefix = "小節の長さが拍子と一致しません:\n"), failures.isEmpty())
    }

    private fun generateNotes(alphatex: String): List<Note> {
        val importer = AlphaTexImporter()
        val settings = Settings()
        importer.initFromString(alphatex, settings)
        val score = importer.readScore()

        val collector = NoteCollector()
        MidiFileGenerator(score, settings, collector).generate()
        return collector.notes
    }

    private fun exerciseFiles(): List<File> {
        val candidates = listOf(File("../content/exercises"), File("content/exercises"))
        val dir = candidates.firstOrNull { it.isDirectory }
            ?: throw AssertionError("content/exercises not found relative to ${File(".").absolutePath}")
        val files = dir.walkTopDown().filter { it.isFile && it.extension == "yaml" }.toList()
        assertTrue("no exercise yaml files found under ${dir.path}", files.isNotEmpty())
        return files
    }

    private companion object {
        /** `フレット.弦.音価`（音符）または `-.弦[.音価]`（タイ）。 */
        val NOTE_OR_TIE = Regex("""(?<![\d.])(?:(\d+)\.([1-6])\.(\d+)|-\.(\d+))""")
        const val NOTE_STRING = 2
        const val TIE_STRING = 4
    }
}

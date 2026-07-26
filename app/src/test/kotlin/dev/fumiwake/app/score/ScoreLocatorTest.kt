package dev.fumiwake.app.score

import alphaTab.Settings
import alphaTab.importer.AlphaTexImporter
import alphaTab.model.Score
import dev.fumiwake.core.exercise.Exercise
import dev.fumiwake.core.exercise.FocusElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [ScoreLocator] built and exercised against a real [Score] parsed by
 * [AlphaTexImporter] — proven to run under plain JVM unit tests
 * (docs/IMPLEMENTATION_PLAN.md §1/§6 M2b), so this locks the bar/beat/note addressing
 * convention without needing a device or emulator.
 *
 * Fixture layout (`\ts 4 4`):
 *  - Bar 0: note, note, rest, dotted note (beats 0..3)
 *  - Bar 1: note, tie continuation (beats 0..1)
 */
class ScoreLocatorTest {

    private val tex = """
        \title "locator test" \tempo 60 . \ts 4 4
        3.3.4 5.3.4 r.4 3.3.4{d} |
        3.4.2 -.2 |
    """.trimIndent()

    private fun buildScore(): Score {
        val importer = AlphaTexImporter()
        importer.initFromString(tex, Settings())
        return importer.readScore()
    }

    @Test
    fun `locate resolves bar and beat index for a note`() {
        val score = buildScore()
        val beat0 = score.tracks.get(0).staves.get(0).bars.get(0).voices.get(0).beats.get(0)
        val note0 = beat0.notes.get(0)

        val loc = ScoreLocator.locate(note0)

        assertEquals(0, loc.barIndex)
        assertEquals(0, loc.beatIndex)
        assertEquals(0, loc.noteIndex)
    }

    @Test
    fun `locate resolves a rest beat with null noteIndex`() {
        val score = buildScore()
        val restBeat = score.tracks.get(0).staves.get(0).bars.get(0).voices.get(0).beats.get(2)

        val loc = ScoreLocator.locate(restBeat)

        assertEquals(0, loc.barIndex)
        assertEquals(2, loc.beatIndex)
        assertNull(loc.noteIndex)
    }

    @Test
    fun `locate resolves beat and note index across bars for a tie continuation`() {
        val score = buildScore()
        val tieBeat = score.tracks.get(0).staves.get(0).bars.get(1).voices.get(0).beats.get(1)
        val tieNote = tieBeat.notes.get(0)

        val loc = ScoreLocator.locate(tieNote)

        assertEquals(1, loc.barIndex)
        assertEquals(1, loc.beatIndex)
        assertEquals(0, loc.noteIndex)
    }

    @Test
    fun `findFocus matches a note-level focus element by bar-beat-note index`() {
        val score = buildScore()
        val note0 = score.tracks.get(0).staves.get(0).bars.get(0).voices.get(0).beats.get(0).notes.get(0)
        val exercise = exerciseWithFocus(
            FocusElement(barIndex = 0, beatIndex = 0, noteIndex = 0, noteText = "first note"),
        )

        val focus = ScoreLocator.findFocus(exercise, note0)

        assertEquals("first note", focus?.noteText)
    }

    @Test
    fun `findFocus matches a beat-level focus element for a rest`() {
        val score = buildScore()
        val restBeat = score.tracks.get(0).staves.get(0).bars.get(0).voices.get(0).beats.get(2)
        val exercise = exerciseWithFocus(
            FocusElement(barIndex = 0, beatIndex = 2, noteIndex = null, noteText = "the rest"),
        )

        val focus = ScoreLocator.findFocus(exercise, restBeat)

        assertEquals("the rest", focus?.noteText)
    }

    @Test
    fun `findFocus returns null when no focus element matches`() {
        val score = buildScore()
        val note1 = score.tracks.get(0).staves.get(0).bars.get(0).voices.get(0).beats.get(1).notes.get(0)
        val exercise = exerciseWithFocus(
            FocusElement(barIndex = 0, beatIndex = 0, noteIndex = 0, noteText = "first note"),
        )

        assertNull(ScoreLocator.findFocus(exercise, note1))
    }

    @Test
    fun `fingeringOf flips alphaTab's string numbering back to the guitarist's`() {
        // フィクスチャの `3.3.4` は alphaTex で「3弦3フレット」。alphaTab のモデル上は
        // string=4（1 = 最低音弦）で入ってくる。ここで反転しないと、指板図も正誤判定も
        // 弦が入れ替わったまま動く。
        val score = buildScore()
        val note = score.tracks.get(0).staves.get(0).bars.get(0).voices.get(0).beats.get(0).notes.get(0)

        val fingering = ScoreLocator.fingeringOf(note)

        assertEquals(3, fingering.string)
        assertEquals(3, fingering.fret)
        // 3弦3フレットの実音は A#3 (58)。alphaTab の realValue と一致するはず。
        assertEquals(58, note.realValue.toInt())
    }

    @Test
    fun `fingeringOf reads the fret from a different string too`() {
        // 2小節目の `3.4.2` = 4弦3フレット（実音 F3 = 53）。
        val score = buildScore()
        val note = score.tracks.get(0).staves.get(0).bars.get(1).voices.get(0).beats.get(0).notes.get(0)

        val fingering = ScoreLocator.fingeringOf(note)

        assertEquals(4, fingering.string)
        assertEquals(3, fingering.fret)
        assertEquals(53, note.realValue.toInt())
    }

    private fun exerciseWithFocus(vararg focusElements: FocusElement) = Exercise(
        exerciseId = "rhythm.beat.999",
        conceptId = "rhythm.beat",
        titleJa = "test",
        alphatex = tex,
        explanationJa = "test",
        focusElements = focusElements.toList(),
        difficulty = 1,
    )
}

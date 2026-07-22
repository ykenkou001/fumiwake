package dev.fumiwake.core.concept

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Asserts `ConceptGraph.bundled()` matches DESIGN.md §2.1 exactly: concept count
 * per domain (25 total) and every dependsOn edge from the taxonomy tables.
 */
class ConceptGraphBundledTest {

    private val graph = ConceptGraph.bundled()

    private fun deps(id: String): List<String> =
        graph.get(id)?.dependsOn ?: error("missing concept: $id")

    @Test
    fun `has exactly 25 concepts`() {
        assertEquals(25, graph.all.size)
    }

    @Test
    fun `has 9 rhythm concepts`() {
        assertEquals(9, graph.all.count { it.id.startsWith("rhythm.") })
    }

    @Test
    fun `has 5 pitch concepts`() {
        assertEquals(5, graph.all.count { it.id.startsWith("pitch.") })
    }

    @Test
    fun `has 4 guitar concepts`() {
        assertEquals(4, graph.all.count { it.id.startsWith("guitar.") })
    }

    @Test
    fun `has 3 structure concepts`() {
        assertEquals(3, graph.all.count { it.id.startsWith("structure.") })
    }

    @Test
    fun `has 4 harmony concepts`() {
        assertEquals(4, graph.all.count { it.id.startsWith("harmony.") })
    }

    @Test
    fun `rhythm dependencies match DESIGN md`() {
        assertEquals(emptyList(), deps("rhythm.beat"))
        assertEquals(listOf("rhythm.beat"), deps("rhythm.basic-values"))
        assertEquals(listOf("rhythm.basic-values"), deps("rhythm.rests"))
        assertEquals(listOf("rhythm.basic-values"), deps("rhythm.dotted"))
        assertEquals(listOf("rhythm.basic-values"), deps("rhythm.tie"))
        assertEquals(listOf("rhythm.basic-values"), deps("rhythm.sixteenth"))
        assertEquals(listOf("rhythm.tie", "rhythm.sixteenth"), deps("rhythm.syncopation"))
        assertEquals(listOf("rhythm.basic-values"), deps("rhythm.triplet"))
        assertEquals(listOf("rhythm.triplet"), deps("rhythm.compound-meter"))
    }

    @Test
    fun `pitch dependencies match DESIGN md`() {
        assertEquals(emptyList(), deps("pitch.staff-names"))
        assertEquals(listOf("pitch.staff-names"), deps("pitch.ledger-lines"))
        assertEquals(listOf("pitch.staff-names"), deps("pitch.accidentals"))
        assertEquals(listOf("pitch.accidentals"), deps("pitch.key-signatures"))
        assertEquals(listOf("pitch.ledger-lines"), deps("pitch.octaves"))
    }

    @Test
    fun `guitar dependencies match DESIGN md`() {
        assertEquals(listOf("pitch.staff-names"), deps("guitar.open-strings"))
        assertEquals(listOf("guitar.open-strings"), deps("guitar.low-position"))
        assertEquals(listOf("guitar.low-position", "pitch.octaves"), deps("guitar.multi-positions"))
        assertEquals(listOf("guitar.low-position"), deps("guitar.tab-mapping"))
    }

    @Test
    fun `structure dependencies match DESIGN md`() {
        assertEquals(listOf("rhythm.beat"), deps("structure.repeats"))
        assertEquals(listOf("structure.repeats"), deps("structure.dc-ds"))
        assertEquals(listOf("rhythm.basic-values"), deps("structure.articulation"))
    }

    @Test
    fun `harmony dependencies match DESIGN md`() {
        assertEquals(listOf("guitar.low-position"), deps("harmony.chords"))
        assertEquals(listOf("pitch.key-signatures"), deps("harmony.keys"))
        assertEquals(listOf("harmony.chords", "harmony.keys"), deps("harmony.diatonic"))
        assertEquals(listOf("harmony.diatonic"), deps("harmony.progressions"))
    }

    @Test
    fun `all concept names are non-blank`() {
        for (concept in graph.all) {
            assert(concept.nameJa.isNotBlank()) { "concept ${concept.id} has blank nameJa" }
        }
    }
}

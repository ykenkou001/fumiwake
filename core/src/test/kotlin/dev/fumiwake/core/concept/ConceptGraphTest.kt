package dev.fumiwake.core.concept

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ConceptGraphTest {

    @Test
    fun `detects a direct cycle`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            ConceptGraph(
                listOf(
                    Concept("a.one", "one", listOf("a.two")),
                    Concept("a.two", "two", listOf("a.one")),
                )
            )
        }
        assertTrue(ex.message!!.contains("cycle"), "message should mention cycle: ${ex.message}")
    }

    @Test
    fun `detects an indirect cycle`() {
        assertFailsWith<IllegalArgumentException> {
            ConceptGraph(
                listOf(
                    Concept("a.one", "one", listOf("a.two")),
                    Concept("a.two", "two", listOf("a.three")),
                    Concept("a.three", "three", listOf("a.one")),
                )
            )
        }
    }

    @Test
    fun `rejects unknown dependency`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            ConceptGraph(listOf(Concept("a.one", "one", listOf("a.missing"))))
        }
        assertTrue(ex.message!!.contains("a.missing"))
    }

    @Test
    fun `rejects malformed id - no dot`() {
        assertFailsWith<IllegalArgumentException> {
            ConceptGraph(listOf(Concept("aone", "one")))
        }
    }

    @Test
    fun `rejects malformed id - uppercase`() {
        assertFailsWith<IllegalArgumentException> {
            ConceptGraph(listOf(Concept("A.one", "one")))
        }
    }

    @Test
    fun `rejects malformed id - domain with hyphen`() {
        assertFailsWith<IllegalArgumentException> {
            ConceptGraph(listOf(Concept("a-b.one", "one")))
        }
    }

    @Test
    fun `accepts id with hyphen and digits in name part`() {
        val graph = ConceptGraph(listOf(Concept("a.step-2", "step 2")))
        assertEquals("step 2", graph.get("a.step-2")?.nameJa)
    }

    @Test
    fun `rejects duplicate ids`() {
        assertFailsWith<IllegalArgumentException> {
            ConceptGraph(
                listOf(
                    Concept("a.one", "one"),
                    Concept("a.one", "one again"),
                )
            )
        }
    }

    @Test
    fun `topological order always places dependencies before dependents`() {
        val graph = ConceptGraph(
            listOf(
                Concept("a.four", "four", listOf("a.two", "a.three")),
                Concept("a.one", "one"),
                Concept("a.three", "three", listOf("a.one")),
                Concept("a.two", "two", listOf("a.one")),
            )
        )
        val order = graph.topologicalOrder().map { it.id }
        val indexOf = order.withIndex().associate { (i, id) -> id to i }
        for (concept in graph.all) {
            for (dep in concept.dependsOn) {
                assertTrue(
                    indexOf.getValue(dep) < indexOf.getValue(concept.id),
                    "expected '$dep' before '${concept.id}' in $order",
                )
            }
        }
    }

    @Test
    fun `topological order is stable - ties broken by definition order`() {
        // b and c both have no deps; d depends on both. Definition order is a, b, c, d.
        val graph = ConceptGraph(
            listOf(
                Concept("x.a", "a"),
                Concept("x.b", "b"),
                Concept("x.c", "c"),
                Concept("x.d", "d", listOf("x.b", "x.c")),
            )
        )
        assertEquals(listOf("x.a", "x.b", "x.c", "x.d"), graph.topologicalOrder().map { it.id })
    }

    @Test
    fun `prerequisitesOf is transitive`() {
        val graph = ConceptGraph(
            listOf(
                Concept("a.one", "one"),
                Concept("a.two", "two", listOf("a.one")),
                Concept("a.three", "three", listOf("a.two")),
            )
        )
        assertEquals(setOf("a.one", "a.two"), graph.prerequisitesOf("a.three"))
        assertEquals(setOf("a.one"), graph.prerequisitesOf("a.two"))
        assertEquals(emptySet(), graph.prerequisitesOf("a.one"))
    }

    @Test
    fun `prerequisitesOf handles diamond dependencies without duplication issues`() {
        val graph = ConceptGraph(
            listOf(
                Concept("a.base", "base"),
                Concept("a.left", "left", listOf("a.base")),
                Concept("a.right", "right", listOf("a.base")),
                Concept("a.top", "top", listOf("a.left", "a.right")),
            )
        )
        assertEquals(setOf("a.base", "a.left", "a.right"), graph.prerequisitesOf("a.top"))
    }

    @Test
    fun `availableConcepts returns concepts whose dependencies are all learned and who are not yet learned`() {
        val graph = ConceptGraph(
            listOf(
                Concept("a.one", "one"),
                Concept("a.two", "two", listOf("a.one")),
                Concept("a.three", "three", listOf("a.two")),
            )
        )
        assertEquals(setOf("a.one"), graph.availableConcepts(emptySet()))
        assertEquals(setOf("a.two"), graph.availableConcepts(setOf("a.one")))
        // once a.two is also learned, a.three becomes available and a.two is excluded
        assertEquals(setOf("a.three"), graph.availableConcepts(setOf("a.one", "a.two")))
    }

    @Test
    fun `get returns null for unknown id`() {
        val graph = ConceptGraph(listOf(Concept("a.one", "one")))
        assertEquals(null, graph.get("a.missing"))
    }
}

package dev.fumiwake.core.concept

/**
 * The notation-concept taxonomy as a DAG (DESIGN.md §2.1).
 *
 * Validates, at construction time, that:
 *  - every concept id matches `^[a-z]+\.[a-z0-9-]+$`
 *  - there are no duplicate ids
 *  - every `dependsOn` entry references a concept that exists in this graph
 *  - the dependency relation contains no cycles
 *
 * Any violation throws [IllegalArgumentException] describing the violation.
 */
class ConceptGraph(concepts: List<Concept>) {

    val all: List<Concept> = concepts.toList()

    private val byId: Map<String, Concept> = all.associateBy { it.id }

    /**
     * Concepts the user is assumed to already know (DESIGN.md §2.3). These count as
     * learned regardless of what the progress store holds — see
     * [dev.fumiwake.core.progress.ProgressEvaluator].
     */
    val assumedKnownIds: Set<String> = all.filter { it.assumedKnown }.map { it.id }.toSet()

    init {
        val seenIds = mutableSetOf<String>()
        for (concept in all) {
            require(ID_PATTERN.matches(concept.id)) {
                "invalid concept id '${concept.id}': must match ${ID_PATTERN.pattern}"
            }
            require(seenIds.add(concept.id)) {
                "duplicate concept id: '${concept.id}'"
            }
        }
        for (concept in all) {
            for (dep in concept.dependsOn) {
                require(byId.containsKey(dep)) {
                    "concept '${concept.id}' depends on unknown concept '$dep'"
                }
            }
        }
        detectCycleOrThrow()
    }

    fun get(id: String): Concept? = byId[id]

    /** Transitive set of all ancestors (dependencies of dependencies, ...) of [id]. */
    fun prerequisitesOf(id: String): Set<String> {
        val result = LinkedHashSet<String>()

        fun visit(conceptId: String) {
            val concept = byId[conceptId] ?: return
            for (dep in concept.dependsOn) {
                if (result.add(dep)) {
                    visit(dep)
                }
            }
        }

        visit(id)
        return result
    }

    /**
     * Default learning-nav order: a stable topological sort where ties (concepts whose
     * dependencies are all already placed) are broken by the taxonomy's definition order.
     */
    fun topologicalOrder(): List<Concept> {
        val remaining = all.toMutableList()
        val placed = mutableSetOf<String>()
        val result = mutableListOf<Concept>()

        while (remaining.isNotEmpty()) {
            val next = remaining.firstOrNull { candidate -> placed.containsAll(candidate.dependsOn) }
                ?: throw IllegalStateException("cycle detected during topological sort (should be unreachable after init validation)")
            result.add(next)
            placed.add(next.id)
            remaining.remove(next)
        }

        return result
    }

    /**
     * Concepts whose full dependency set is satisfied by [learned], excluding already-learned ones.
     *
     * [learned] must be the *effective* learned set, i.e. include [assumedKnownIds];
     * this function takes the set as given and does not add them itself.
     */
    fun availableConcepts(learned: Set<String>): Set<String> =
        all.asSequence()
            .filter { it.id !in learned }
            .filter { learned.containsAll(it.dependsOn) }
            .map { it.id }
            .toSet()

    private fun detectCycleOrThrow() {
        val state = HashMap<String, Int>() // 0 (default/unvisited) -> 1 visiting -> 2 done
        val stack = mutableListOf<String>()

        fun dfs(id: String) {
            state[id] = 1
            stack.add(id)
            val concept = byId.getValue(id)
            for (dep in concept.dependsOn) {
                when (state[dep] ?: 0) {
                    0 -> dfs(dep)
                    1 -> {
                        val cycleStart = stack.indexOf(dep)
                        val cycle = stack.subList(cycleStart, stack.size) + dep
                        throw IllegalArgumentException(
                            "cycle detected in concept graph: ${cycle.joinToString(" -> ")}"
                        )
                    }
                    else -> Unit
                }
            }
            state[id] = 2
            stack.removeAt(stack.size - 1)
        }

        for (concept in all) {
            if (state[concept.id] != 2) {
                dfs(concept.id)
            }
        }
    }

    companion object {
        private val ID_PATTERN = Regex("^[a-z]+\\.[a-z0-9-]+$")

        /** DESIGN.md §2.1: the full 25-concept bundled taxonomy across all five domains. */
        fun bundled(): ConceptGraph = ConceptGraph(
            listOf(
                // rhythm (9)
                Concept("rhythm.beat", "拍と拍子（4/4の意味）"),
                Concept("rhythm.basic-values", "音価の基本（全〜8分）", listOf("rhythm.beat")),
                Concept("rhythm.rests", "休符", listOf("rhythm.basic-values")),
                Concept("rhythm.dotted", "付点", listOf("rhythm.basic-values")),
                Concept("rhythm.tie", "タイ", listOf("rhythm.basic-values")),
                Concept("rhythm.sixteenth", "16分音符", listOf("rhythm.basic-values")),
                Concept("rhythm.syncopation", "シンコペーション", listOf("rhythm.tie", "rhythm.sixteenth")),
                Concept("rhythm.triplet", "3連符", listOf("rhythm.basic-values")),
                Concept("rhythm.compound-meter", "複合拍子（6/8等）", listOf("rhythm.triplet")),

                // pitch (5)
                // 対象ユーザー（既に譜面がある程度読める）向けにエクササイズを作る意味が
                // ないため、assumedKnown で既習として扱う。これが無いと guitar ドメイン
                // 全体が永久に LOCKED になる（DESIGN.md §2.3）。
                Concept("pitch.staff-names", "五線上の音名（既習）", assumedKnown = true),
                Concept("pitch.ledger-lines", "加線", listOf("pitch.staff-names")),
                Concept("pitch.accidentals", "♯♭♮（臨時記号）", listOf("pitch.staff-names")),
                Concept("pitch.key-signatures", "調号", listOf("pitch.accidentals")),
                Concept("pitch.octaves", "オクターブの読み分け", listOf("pitch.ledger-lines")),

                // guitar (4)
                Concept("guitar.open-strings", "開放弦と五線の対応", listOf("pitch.staff-names")),
                Concept("guitar.low-position", "ローポジションの音符→フレット", listOf("guitar.open-strings")),
                Concept(
                    "guitar.multi-positions",
                    "同じ音の複数ポジション",
                    listOf("guitar.low-position", "pitch.octaves"),
                ),
                Concept("guitar.tab-mapping", "TAB譜⇄五線譜の読み替え", listOf("guitar.low-position")),

                // structure (3)
                Concept("structure.repeats", "反復記号（リピート、1・2番括弧）", listOf("rhythm.beat")),
                Concept("structure.dc-ds", "D.C. / D.S. / Coda", listOf("structure.repeats")),
                Concept("structure.articulation", "スラー・スタッカート等", listOf("rhythm.basic-values")),

                // harmony (4)
                Concept("harmony.chords", "コード記号", listOf("guitar.low-position")),
                Concept("harmony.keys", "キーと調号の対応", listOf("pitch.key-signatures")),
                Concept("harmony.diatonic", "ダイアトニックコード", listOf("harmony.chords", "harmony.keys")),
                Concept("harmony.progressions", "進行の型", listOf("harmony.diatonic")),
            )
        )
    }
}

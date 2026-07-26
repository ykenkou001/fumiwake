@file:OptIn(ExperimentalMaterial3Api::class)

package dev.fumiwake.app.ui

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.fumiwake.app.AppState
import dev.fumiwake.app.SpikeActivity
import dev.fumiwake.core.concept.Concept
import dev.fumiwake.core.concept.ConceptGraph
import dev.fumiwake.core.progress.ConceptStatus

/**
 * Display labels for the taxonomy's domain codes (docs/IMPLEMENTATION_PLAN.md §2.1);
 * this is purely presentational, so it stays in `:app` rather than `:core`.
 */
private val DOMAIN_LABELS = mapOf(
    "rhythm" to "リズム",
    "pitch" to "音高",
    "guitar" to "ギター",
    "structure" to "曲の構造",
    "harmony" to "ハーモニー",
)

/**
 * Home screen (docs/IMPLEMENTATION_PLAN.md §5.1): domains that have at least one bundled
 * exercise ([Catalog.domains]), each listing its concepts in topological order with a
 * LOCKED/AVAILABLE/LEARNED badge. Concepts are always tappable, even when LOCKED — the
 * badge is guidance, not a gate.
 */
@Composable
fun ConceptMapScreen(
    appState: AppState,
    onConceptClick: (String) -> Unit,
) {
    val context = LocalContext.current
    // Reading progressTick here (even unused otherwise) makes this screen recompose,
    // and thus re-read statusOf(...) below, whenever ExerciseScreen records progress.
    val progressTick = appState.progressTick.intValue

    Scaffold(
        topBar = { TopAppBar(title = { Text("ふみわけ") }) },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxWidth()) {
            for (domain in appState.catalog.domains) {
                item(key = "header-$domain") {
                    Text(
                        text = DOMAIN_LABELS[domain] ?: domain,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                // LazyListScope content is not a composable context, so remember{} is
                // illegal here; the lookup is cheap and pure, so recompute directly.
                val concepts = appState.catalog.conceptsInDomain(domain)
                items(concepts, key = { it.id }) { concept ->
                    ConceptCard(
                        concept = concept,
                        status = appState.progressEvaluator.statusOf(concept.id),
                        graph = appState.graph,
                        onClick = { onConceptClick(concept.id) },
                    )
                }
            }
            item(key = "spike-link") {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                TextButton(
                    onClick = { context.startActivity(Intent(context, SpikeActivity::class.java)) },
                ) {
                    Text("デバッグ: スパイク画面")
                }
            }
            item(key = "version-footer") {
                // versionName は CI が 0.1.<ラン番号> で採番する（app/build.gradle.kts）。
                // 端末に入っているビルドがどの CI ランのものか、配布トラブル時に
                // ここを見れば特定できる。
                val versionName = remember {
                    runCatching {
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName
                    }.getOrNull() ?: "不明"
                }
                Text(
                    text = "バージョン: $versionName",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ConceptCard(
    concept: Concept,
    status: ConceptStatus,
    graph: ConceptGraph,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("${badgeFor(concept, status)} ${concept.nameJa}")
            if (concept.dependsOn.isNotEmpty()) {
                val depNames = concept.dependsOn.joinToString("、") { graph.get(it)?.nameJa ?: it }
                Text(
                    text = "前提: $depNames",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/**
 * An assumed-known concept (DESIGN.md §2.3) evaluates to LEARNED, but the user did not
 * learn it here — the app merely assumes it. Showing the same ✓ as a concept they worked
 * through would misreport their own progress back to them, so it gets its own badge.
 */
private fun badgeFor(concept: Concept, status: ConceptStatus): String =
    if (concept.assumedKnown) "既習" else statusBadge(status)

private fun statusBadge(status: ConceptStatus): String = when (status) {
    ConceptStatus.LOCKED -> "🔒"
    ConceptStatus.AVAILABLE -> "▶"
    ConceptStatus.LEARNED -> "✓"
}

package dev.fumiwake.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.fumiwake.app.AppState

/**
 * Exercises for one concept, difficulty-ordered ([dev.fumiwake.core.catalog.Catalog.byConcept]
 * is already sorted). Header shows the concept name and, if any, its prerequisite
 * concepts by name (docs/IMPLEMENTATION_PLAN.md §5.2).
 */
@Composable
fun ExerciseListScreen(
    appState: AppState,
    conceptId: String,
    onExerciseClick: (String) -> Unit,
    onBack: () -> Unit,
) {
    val concept = appState.graph.get(conceptId)
    val exercises = appState.catalog.byConcept(conceptId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(concept?.nameJa ?: conceptId) },
                navigationIcon = { TextButton(onClick = onBack) { Text("戻る") } },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxWidth()) {
            if (concept != null && concept.dependsOn.isNotEmpty()) {
                item(key = "prerequisites") {
                    val depNames = concept.dependsOn.joinToString("、") {
                        appState.graph.get(it)?.nameJa ?: it
                    }
                    Text(
                        text = "前提概念: $depNames",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(exercises, key = { it.exerciseId }) { exercise ->
                Card(
                    onClick = { onExerciseClick(exercise.exerciseId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(exercise.titleJa, style = MaterialTheme.typography.titleMedium)
                        Text("難易度: ${"★".repeat(exercise.difficulty)}${"☆".repeat(5 - exercise.difficulty)}")
                    }
                }
            }
        }
    }
}

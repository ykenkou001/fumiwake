package dev.fumiwake.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import dev.fumiwake.app.data.AssetExerciseRepository
import dev.fumiwake.app.data.SqliteProgressStore
import dev.fumiwake.app.ui.FumiwakeNavHost
import dev.fumiwake.core.concept.ConceptGraph
import dev.fumiwake.core.progress.ProgressEvaluator

/**
 * M2b entry point: the full Compose UI (concept map -> exercise list -> exercise screen,
 * docs/IMPLEMENTATION_PLAN.md §5/§6 M2b). The M2a spike screen that used to live here now
 * lives on as [SpikeActivity], reachable via a small debug link at the bottom of the
 * concept map.
 *
 * The exercise [dev.fumiwake.core.catalog.Catalog] is loaded from assets asynchronously
 * (never blocking onCreate); a spinner is shown until it's ready.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = AssetExerciseRepository(applicationContext)
        val progressStore = SqliteProgressStore(applicationContext)

        setContent {
            var appState by remember { mutableStateOf<AppState?>(null) }

            LaunchedEffect(Unit) {
                val graph = ConceptGraph.bundled()
                val catalog = repository.load()
                val evaluator = ProgressEvaluator(graph, catalog, progressStore)
                appState = AppState(graph, catalog, evaluator)
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val state = appState
                    if (state == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val navController = rememberNavController()
                        FumiwakeNavHost(navController = navController, appState = state)
                    }
                }
            }
        }
    }
}

package dev.fumiwake.app

import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import dev.fumiwake.app.data.PlaybackPreferences
import dev.fumiwake.core.catalog.Catalog
import dev.fumiwake.core.concept.ConceptGraph
import dev.fumiwake.core.progress.ProgressEvaluator

/**
 * The app's small amount of shared state, built once in [MainActivity] after
 * [dev.fumiwake.app.data.AssetExerciseRepository.load] returns and handed down to every
 * screen (docs/IMPLEMENTATION_PLAN.md §5).
 *
 * [progressTick] is bumped by [notifyProgressChanged] whenever progress is recorded
 * (ExerciseScreen's "complete" button); screens that read `progressTick.intValue` (even
 * without using the value itself) recompose and re-read [progressEvaluator], since the
 * underlying [dev.fumiwake.core.progress.ProgressStore] has no change-notification of
 * its own.
 *
 * [playbackPreferences] holds the app-wide playback options (metronome / count-in) that
 * must survive exercise switches and restarts — docs/METRONOME_PLAN.md §2.2.
 */
class AppState(
    val graph: ConceptGraph,
    val catalog: Catalog,
    val progressEvaluator: ProgressEvaluator,
    val playbackPreferences: PlaybackPreferences,
) {
    val progressTick: MutableIntState = mutableIntStateOf(0)

    fun notifyProgressChanged() {
        progressTick.intValue++
    }
}

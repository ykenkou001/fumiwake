@file:OptIn(ExperimentalMaterial3Api::class)

package dev.fumiwake.app.ui

import alphaTab.AlphaTabApiBase
import alphaTab.AlphaTabView
import alphaTab.model.Beat
import alphaTab.model.Note
import alphaTab.synth.PlayerState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.fumiwake.app.AppState
import dev.fumiwake.app.score.ExerciseScoreView
import dev.fumiwake.app.score.ScoreLocator
import dev.fumiwake.core.music.DurationTexts
import dev.fumiwake.core.music.NoteNames
import dev.fumiwake.core.progress.ConceptStatus
import kotlinx.coroutines.launch

/**
 * The core screen (docs/IMPLEMENTATION_PLAN.md §5.3): score + TAB on top, tap-to-explain
 * panel and playback controls below, and a "complete this exercise" action that reports
 * back through [dev.fumiwake.core.progress.ProgressEvaluator].
 */
@Composable
fun ExerciseScreen(
    appState: AppState,
    exerciseId: String,
    onBack: () -> Unit,
) {
    val exercise = appState.catalog.byId(exerciseId)
    if (exercise == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("エラー") },
                    navigationIcon = { TextButton(onClick = onBack) { Text("戻る") } },
                )
            },
        ) { padding ->
            Text(
                text = "エクササイズが見つかりません: $exerciseId",
                modifier = Modifier.padding(padding).padding(16.dp),
            )
        }
        return
    }

    val concept = appState.graph.get(exercise.conceptId)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var playbackSpeed by remember(exerciseId) { mutableFloatStateOf(1.0f) }
    var isLooping by remember(exerciseId) { mutableStateOf(false) }
    var playerState by remember(exerciseId) { mutableStateOf(PlayerState.Paused) }
    var isPlayerReady by remember(exerciseId) { mutableStateOf(false) }
    var panelText by remember(exerciseId) { mutableStateOf(exercise.explanationJa) }
    val apiState = remember(exerciseId) { mutableStateOf<AlphaTabApiBase<AlphaTabView>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercise.titleJa) },
                navigationIcon = { TextButton(onClick = onBack) { Text("戻る") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ExerciseScoreView(
                exercise = exercise,
                playbackSpeed = playbackSpeed,
                isLooping = isLooping,
                apiState = apiState,
                onReady = { isPlayerReady = true },
                onNoteTap = { note ->
                    panelText = ScoreLocator.findFocus(exercise, note)?.noteText
                        ?: genericNoteInfo(note)
                    apiState.value?.playNote(note)
                },
                onBeatTap = { beat ->
                    if (beat.isRest) {
                        panelText = ScoreLocator.findFocus(exercise, beat)?.noteText
                            ?: genericRestInfo(beat)
                    }
                },
                onPlayerStateChanged = { state -> playerState = state },
                onError = { throwable -> panelText = "エラー: ${throwable.message}" },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Button(
                    enabled = isPlayerReady,
                    onClick = { apiState.value?.playPause() },
                ) {
                    Text(if (playerState == PlayerState.Playing) "停止" else "再生")
                }
                Text(
                    text = "ループ",
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp),
                )
                Switch(
                    checked = isLooping,
                    onCheckedChange = { isLooping = it },
                )
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text("テンポ: ${(playbackSpeed * 100).toInt()}%")
                Slider(
                    value = playbackSpeed,
                    onValueChange = { playbackSpeed = it },
                    valueRange = 0.5f..1.0f,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Text(panelText, style = MaterialTheme.typography.bodyLarge)
            }

            Button(
                onClick = {
                    val newStatus = appState.progressEvaluator.recordCompletion(exercise.exerciseId)
                    appState.notifyProgressChanged()
                    scope.launch {
                        val message = if (newStatus == ConceptStatus.LEARNED) {
                            "${concept?.nameJa ?: exercise.conceptId} を習得しました！"
                        } else {
                            "完了を記録しました"
                        }
                        snackbarHostState.showSnackbar(message)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text("このエクササイズを完了")
            }
        }
    }
}

/** Fallback tap-detail text for a note with no matching [dev.fumiwake.core.exercise.FocusElement]. */
private fun genericNoteInfo(note: Note): String {
    val beat = note.beat
    val noteName = NoteNames.fromMidi(note.realValue.toInt())
    val durationText = runCatching {
        DurationTexts.of(beat.duration.value, beat.dots.toInt(), isRest = false)
    }.getOrDefault("音価不明")
    return "$noteName / $durationText（フレット${note.fret.toInt()}・${note.string.toInt()}弦）"
}

/** Fallback tap-detail text for a rest beat with no matching FocusElement. */
private fun genericRestInfo(beat: Beat): String = runCatching {
    DurationTexts.of(beat.duration.value, beat.dots.toInt(), isRest = true)
}.getOrDefault("休符")

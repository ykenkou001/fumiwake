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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.fumiwake.app.AppState
import dev.fumiwake.app.score.ExerciseScoreView
import dev.fumiwake.app.score.ScoreLocator
import dev.fumiwake.core.music.DurationTexts
import dev.fumiwake.core.music.NoteNames
import dev.fumiwake.core.progress.ConceptStatus
import kotlinx.coroutines.delay
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
    var playerStatus by remember(exerciseId) { mutableStateOf<PlayerStatus>(PlayerStatus.Loading) }
    var panelText by remember(exerciseId) { mutableStateOf(exercise.explanationJa) }
    val apiState = remember(exerciseId) { mutableStateOf<AlphaTabApiBase<AlphaTabView>?>(null) }

    // 音源（サウンドフォント）・シンセの初期化は通常すぐ終わる。一定時間 ready にも
    // error にもならなければ「無言のまま音が出ない」状態とみなし、ユーザーに知らせる。
    // alphaTab はロード失敗を api.error に funnel するので Failed はそちらで拾うが、
    // 何のイベントも来ないケース（サイレント失敗）はこのウォッチドッグで拾う。
    LaunchedEffect(exerciseId) {
        delay(READY_TIMEOUT_MS)
        if (playerStatus is PlayerStatus.Loading) {
            playerStatus = PlayerStatus.Slow
        }
    }

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
                onReady = { playerStatus = PlayerStatus.Ready },
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
                // 音源/シンセのロード失敗は alphaTab が api.error に funnel する。
                // 解説パネル（panelText）はタップ解説専用に戻し、失敗は再生ボタン近傍の
                // 専用ステータスで見せる（見落とし・上書きを防ぐ）。
                onError = { throwable ->
                    playerStatus = PlayerStatus.Failed(throwable.message ?: "不明なエラー")
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )

            // 音源準備の進行・失敗を再生ボタンの直上に見せる（無言失敗の可視化）。
            when (val status = playerStatus) {
                PlayerStatus.Loading -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Text(
                        text = "音源を準備中…",
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                PlayerStatus.Slow -> Text(
                    text = "音源の読み込みに時間がかかっています。改善しない場合はアプリを再起動してください。",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                is PlayerStatus.Failed -> Text(
                    text = "音声の初期化に失敗しました: ${status.message}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                PlayerStatus.Ready -> Unit
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Button(
                    enabled = playerStatus is PlayerStatus.Ready,
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

            // 解説テキストは高さを抑え（必要ならスクロール）、余った縦幅を上の譜面
            // （weight(1f)）に回して譜面を見やすくする。以前は譜面と解説が weight(1f)
            // ずつで半々だった。
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 132.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(panelText, style = MaterialTheme.typography.bodyMedium)
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

/**
 * 音源（サウンドフォント）＋シンセの準備状態。再生ボタンの活性と、ユーザーへの
 * ステータス表示（無言失敗の可視化）に使う。
 */
private sealed interface PlayerStatus {
    /** 初期化中（playerReady も error もまだ）。 */
    data object Loading : PlayerStatus

    /** 再生可能（api.playerReady 発火）。 */
    data object Ready : PlayerStatus

    /** 初期化失敗（api.error 由来。alphaTab がロード失敗を funnel する）。 */
    data class Failed(val message: String) : PlayerStatus

    /** 一定時間 準備完了もエラーも来ない（サイレント失敗の疑い）。 */
    data object Slow : PlayerStatus
}

/** 準備完了を待つ上限。これを超えて Loading のままなら [PlayerStatus.Slow] に遷移。 */
private const val READY_TIMEOUT_MS = 8_000L

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

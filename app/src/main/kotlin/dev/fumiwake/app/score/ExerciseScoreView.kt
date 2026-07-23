package dev.fumiwake.app.score

import alphaTab.AlphaTabApiBase
import alphaTab.AlphaTabView
import alphaTab.PlayerMode
import alphaTab.model.Beat
import alphaTab.model.Note
import alphaTab.synth.PlayerState
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import dev.fumiwake.app.R
import dev.fumiwake.core.exercise.Exercise

/**
 * Embeds alphaTab's [AlphaTabView] into Compose via [AndroidView], reusing the
 * settings/event-wiring pattern proven working on-device in the M2a spike
 * ([dev.fumiwake.app.SpikeActivity]) — see docs/alphatab-api-notes.md and
 * docs/IMPLEMENTATION_PLAN.md §5.
 *
 * The view and its `api` are created once, in [AndroidView]'s `factory`; [apiState] is
 * then populated with the live [AlphaTabApiBase] so the caller (ExerciseScreen) can drive
 * playback (play/pause, tempo, loop) imperatively from button/slider callbacks.
 * [playbackSpeed] and [isLooping] changes are re-applied on every recomposition via the
 * `update` block, so the caller can just treat them as normal Compose state.
 */
@Composable
fun ExerciseScoreView(
    exercise: Exercise,
    playbackSpeed: Float,
    isLooping: Boolean,
    apiState: MutableState<AlphaTabApiBase<AlphaTabView>?>,
    onReady: () -> Unit,
    onNoteTap: (Note) -> Unit,
    onBeatTap: (Beat) -> Unit,
    onPlayerStateChanged: (PlayerState) -> Unit,
    onError: (Throwable) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The factory lambda below runs exactly once (on first composition of this node), so
    // any lambda it captures directly would go stale across recomposition (e.g. after
    // ExerciseScreen's state changes). rememberUpdatedState keeps the captured references
    // always pointing at the latest lambda passed in.
    val currentOnReady by rememberUpdatedState(onReady)
    val currentOnNoteTap by rememberUpdatedState(onNoteTap)
    val currentOnBeatTap by rememberUpdatedState(onBeatTap)
    val currentOnPlayerStateChanged by rememberUpdatedState(onPlayerStateChanged)
    val currentOnError by rememberUpdatedState(onError)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val view = LayoutInflater.from(context)
                .inflate(R.layout.view_exercise_score, null) as AlphaTabView

            // parent=null の inflate は XML の match_parent (LayoutParams) を捨てるため、
            // Compose の AndroidView 側で wrap_content 扱いになり AT_MOST で測定される。
            // すると AlphaTabView 内部の outerScroll が中身（初期サイズ 0 の
            // AlphaTabRenderSurface）に収縮して実測幅 0 となり、alphaTab は
            // "width=0" で描画をスキップ、リサイズ再描画も 0→0 で発火せず譜面が
            // 永遠に真っ白になる（音声は純 Kotlin なので再生だけは動く）。
            // MATCH_PARENT を明示して Compose が割り当てた実サイズで測定させる。
            view.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )

            // Same PlayerSettings as the M2a spike: playback must be opted into
            // explicitly (PlayerMode defaults to Disabled). The bundled sonivox
            // soundfont and Bravura music font ship inside the alphaTab AAR's own
            // assets/ and are picked up automatically — no manual loadSoundFont() call.
            val settings = view.settings
            settings.player.enablePlayer = true
            settings.player.playerMode = PlayerMode.EnabledSynthesizer
            settings.player.enableCursor = true
            settings.player.enableUserInteraction = true
            // bufferTimeInMilliseconds は alphaTab 既定（500ms）のまま使う。150ms まで
            // 詰めるとバッファ不足でオーディオ出力がアンダーランして復帰せず、音が止まり、
            // 実再生サンプル数で駆動される再生カーソルも途中で凍結する回帰が起きたため。
            view.settings = settings

            val api = view.api
            api.updateSettings()

            // 一部エクササイズ（全音符・タイなど長い音が多い譜例）で、サステイン音が
            // マスター音量 1.0 だとクリップして「ビィィィン」という歪みになる。
            // ヘッドルームを確保してクリップを防ぐ。
            api.masterVolume = MASTER_VOLUME

            api.error.on { throwable -> currentOnError(throwable) }
            api.playerReady.on { currentOnReady() }
            api.playerStateChanged.on { args -> currentOnPlayerStateChanged(args.state) }
            // noteMouseDown / beatMouseDown: alphaTab's own AndroidViewContainer already
            // converts the touch event into a boundsLookup hit internally (see
            // dev.fumiwake.app.SpikeActivity) — no manual coordinate math needed here.
            api.noteMouseDown.on { note -> currentOnNoteTap(note) }
            api.beatMouseDown.on { beat -> currentOnBeatTap(beat) }

            api.playbackSpeed = playbackSpeed.toDouble()
            api.isLooping = isLooping

            apiState.value = api
            api.tex(exercise.alphatex)

            // alphaTab 初期化競合（幅0でのスキップ後に再描画が発火しない）対策。
            // 詳細は AlphaTabRenderFix.kt のコメント参照。
            view.renderWhenSized()

            view
        },
        update = { view ->
            val api = view.api
            api.playbackSpeed = playbackSpeed.toDouble()
            api.isLooping = isLooping
        },
        // コンポジション離脱（画面を離れる／エクササイズ切替）時にプレイヤーと
        // シンセ（オーディオスレッド・サウンドフォント等）を明示的に解放する。
        // これが無いとビュー detach 頼みになり、リソースが残るリークの懸念がある。
        // runCatching で二重破棄・未初期化状態を吸収する。破棄後に detach 済みの
        // ビューへ renderWhenSized 経由で api.render() が呼ばれることは、
        // 同リスナーの isAttachedToWindow ガードにより起きない。
        onRelease = { view -> runCatching { view.api.destroy() } },
    )
}

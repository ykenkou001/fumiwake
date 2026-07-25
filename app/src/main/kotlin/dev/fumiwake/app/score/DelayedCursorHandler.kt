package dev.fumiwake.app.score

import alphaTab.ICursorHandler
import alphaTab.midi.MidiTickLookupFindBeatResultCursorMode
import alphaTab.platform.Cursors
import alphaTab.platform.IContainer
import alphaTab.rendering.utils.BeatBounds
import android.os.Handler
import android.os.Looper

/**
 * 再生カーソルの表示を一定時間遅らせるための [ICursorHandler] ラッパ
 * （docs/AUDIO_DEFECT_PLAN.md §25）。
 *
 * カーソルは出力へ引き渡したサンプル数で駆動されるため、実際に耳に届く音より先行する。
 * 内部バッファぶんの先行は [AUDIO_BUFFER_MS] を縮めれば減らせるが、Bluetooth の場合は
 * 伝送遅延（機種・コーデックにより 150〜250ms 程度）が上乗せされ、これは音を送り出して
 * から耳に届くまでの物理的な遅れなのでバッファをいくら削っても消えない。
 *
 * そこで残りは**カーソル側を遅らせて辻褄を合わせる**。遅延量は環境によって違うため
 * ユーザーが画面から調整でき、[dev.fumiwake.app.data.PlaybackPreferences] に保存される。
 *
 * 描画そのものは alphaTab 既定の実装（[inner]）に委ね、呼び出しを遅らせるだけにして
 * ある。カーソルの見た目・アニメーションは変わらない。
 */
class DelayedCursorHandler(private val inner: ICursorHandler) : ICursorHandler {

    /** 表示を遅らせる量（ミリ秒）。0 なら遅延なしで即座に委譲する。 */
    @Volatile
    var delayMillis: Long = 0

    private val handler = Handler(Looper.getMainLooper())

    // onAttach/onDetach は描画位置ではなく内部状態の準備・後始末なので遅らせない。
    override fun onAttach(cursors: Cursors) = inner.onAttach(cursors)

    override fun onDetach(cursors: Cursors) {
        // 画面を離れたあとに遅延実行が走ると、破棄済みのビューへ触りに行くことになる。
        handler.removeCallbacksAndMessages(null)
        inner.onDetach(cursors)
    }

    override fun placeBarCursor(container: IContainer, bounds: BeatBounds) {
        post { inner.placeBarCursor(container, bounds) }
    }

    override fun placeBeatCursor(container: IContainer, bounds: BeatBounds, beatX: Double) {
        post { inner.placeBeatCursor(container, bounds, beatX) }
    }

    override fun transitionBeatCursor(
        container: IContainer,
        bounds: BeatBounds,
        beatX: Double,
        nextBeatX: Double,
        duration: Double,
        cursorMode: MidiTickLookupFindBeatResultCursorMode,
    ) {
        post { inner.transitionBeatCursor(container, bounds, beatX, nextBeatX, duration, cursorMode) }
    }

    private fun post(action: () -> Unit) {
        val delay = delayMillis
        if (delay <= 0L) {
            action()
        } else {
            handler.postDelayed(action, delay)
        }
    }
}

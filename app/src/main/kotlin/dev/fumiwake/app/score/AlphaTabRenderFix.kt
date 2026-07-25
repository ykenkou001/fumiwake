package dev.fumiwake.app.score

import alphaTab.AlphaTabApiBase
import alphaTab.AlphaTabView
import alphaTab.synth.PlayerState

/**
 * サステイン音（全音符・タイなど）がクリップして歪む（「ビィィィン」）のを防ぐための
 * ヘッドルーム。全画面共通で使う。
 *
 * 歪みの主因は音量ではなくタイ記法の誤りによるゴースト音だった
 * （docs/AUDIO_DEFECT_PLAN.md §1）。ただし全譜例が同じ低音の連打で書かれており、
 * 減衰しきる前の再発音が重なってピークが伸びる余地は残る（同 §2）ため、
 * 1.0→0.8 で足りなかったヘッドルームをさらに広げる。
 */
const val MASTER_VOLUME: Double = 0.6

/**
 * メトロノーム（クリック）の音量。譜面の音が [MASTER_VOLUME] で頭を抑えてあるため、
 * クリックが埋もれない程度に確保する。実機で「大きすぎ／小さすぎ」が判明したときに
 * ここだけ直せばよいように定数化している（docs/METRONOME_PLAN.md R1）。
 */
const val METRONOME_VOLUME: Double = 0.8

/** カウントイン（再生前の予備カウント）の音量。クリックと揃える。 */
const val COUNT_IN_VOLUME: Double = 0.8

/**
 * クリックとカウントインの ON/OFF を alphaTab へ反映する。
 * alphaTab は音量 0.0 を「鳴らさない」として扱うので、真偽値と音量の対応は
 * ここに一本化する（docs/METRONOME_PLAN.md §2.3）。
 */
fun AlphaTabApiBase<AlphaTabView>.applyClickVolumes(
    metronomeEnabled: Boolean,
    countInEnabled: Boolean,
) {
    metronomeVolume = if (metronomeEnabled) METRONOME_VOLUME else 0.0
    countInVolume = if (countInEnabled) COUNT_IN_VOLUME else 0.0
}

/**
 * alphaTab 1.8.4 の Android 実装にある初期化競合の回避策。
 *
 * `api.tex()` はレイアウト前に呼ばれると幅 0 でワーカーに描画指示を出し、コアは
 * "width=0" で描画をスキップする。回復はレイアウト後の resize イベントが
 * 「container.width != renderer.width（UI側キャッシュ）」を検知して再描画する
 * 仕組みだけに依存しているが、alphaTab 内部の initialRender（Handler.post で
 * 遅延実行）が「レイアウト完了後〜throttled resize 判定（25ms 遅延）発火前」の
 * 窓で走ると、幅キャッシュだけが実幅に同期され描画は発行されない（AlphaTabView.tracks
 * は tex() 経路では null のため renderTracks() が no-op）。その場合 resize 判定は
 * 幅一致と誤認して再描画せず、譜面が恒久的に白紙のままになる。タイミング依存の
 * ため「表示されるときとされない時がある」症状として現れる。
 *
 * `api.render()` は container の現在幅を読み直して renderer に反映した上で
 * 保存済みスコアを full render するため、レイアウト確定後（幅 > 0）に呼べば
 * どの競合状態からでも正しい描画に到達する。このリスナーは幅が変化して正の値に
 * なるたびにそれを行う。正常経路と重なった場合は full render が 1 回余分に走るが、
 * 結果は冪等で実害はない。
 */
fun AlphaTabView.renderWhenSized() {
    addOnLayoutChangeListener { v, left, _, right, _, oldLeft, _, oldRight, _ ->
        val width = right - left
        val oldWidth = oldRight - oldLeft
        if (width > 0 && width != oldWidth) {
            // レイアウトパス中の再入を避けるため post してから描画を指示する
            v.post {
                // 再生中の full render は boundsLookup を作り直し、実再生サンプル数で
                // 駆動される再生カーソルを途中に取り残して停止させることがある。
                // 再生中は再描画をスキップし、幅変化の反映は再生停止後に委ねる。
                if (v.isAttachedToWindow && api.playerState != PlayerState.Playing) {
                    runCatching { api.render() }
                }
            }
        }
    }
}

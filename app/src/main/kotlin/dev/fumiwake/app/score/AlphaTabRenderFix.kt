package dev.fumiwake.app.score

import alphaTab.AlphaTabApiBase
import alphaTab.AlphaTabView
import alphaTab.synth.PlayerState

/**
 * サステイン音（全音符・タイなど）のヘッドルーム。全画面共通で使う。
 *
 * かつての「ビィィィン」はタイ記法の誤りによるゴースト音が原因で、音量とは無関係
 * だった（docs/AUDIO_DEFECT_PLAN.md §1）。合成音を JVM 上で実測したところ、この値が
 * 0.6 のときピーク 0.219・クリップ 0 サンプルで、頭を抑える必要はなかった（同 §11.4）。
 * 一時 0.6 まで下げたが根拠を失ったため 0.8 に戻す。
 */
const val MASTER_VOLUME: Double = 0.8

/**
 * オーディオバッファ長の既定値。**エクササイズ画面ではユーザーが画面から調整する**ので
 * （[dev.fumiwake.app.data.PlaybackPreferences.audioBufferMs]）、この定数を使うのは
 * 設定 UI を持たないスパイク画面（[dev.fumiwake.app.SpikeActivity]）だけ。
 *
 * この値は**音の途切れと再生カーソルのズレのトレードオフ点**を決める
 * （docs/AUDIO_DEFECT_PLAN.md §20）。
 *
 * alphaTab 1.8.4 は `AudioTrack` を **4096 バイト固定**（ステレオ float で 512 フレーム
 * ＝ 44.1kHz で約 11.6ms）で生成し、`AudioTrack.getMinBufferSize()` を参照しない。
 * 内蔵スピーカーは低遅延経路なので成立するが、Bluetooth は符号化・パケット化のため
 * 桁違いに大きなバッファを要するので枯渇し、周期的なうねりになる（同 §16）。
 * この 4096 はライブラリ内の固定値で外から変えられず、本設定が効くのは内部の
 * CircularSampleBuffer だけ。
 *
 * 実機での確認結果:
 *  - 150ms: アンダーランから復帰せず無音・カーソル凍結（過去の回帰）
 *  - 500ms（alphaTab 既定）: Bluetooth でうねる
 *  - 1000ms: Bluetooth のうねりはほぼ解消するが、カーソルが音よりかなり先行する
 *  - **700ms（現在）**: 上記の中間を狙う
 *
 * 内部バッファは 4096 サンプル単位に量子化される（`bufferTime * 44100 / 1000 / 4096` の
 * 整数部がブロック数）ため、1000ms→10 ブロック、700ms→7 ブロックとなり約 3 割短くなる。
 */
const val AUDIO_BUFFER_MS: Double = 700.0

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

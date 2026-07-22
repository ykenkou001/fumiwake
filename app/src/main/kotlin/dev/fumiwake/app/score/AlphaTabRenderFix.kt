package dev.fumiwake.app.score

import alphaTab.AlphaTabView

/**
 * サステイン音（全音符・タイなど）がマスター音量 1.0 でクリップして歪む
 * （「ビィィィン」）のを防ぐためのヘッドルーム。全画面共通で使う。
 */
const val MASTER_VOLUME: Double = 0.8

/**
 * オーディオ出力バッファ長。alphaTab 既定は 500ms で、再生カーソルと実際に
 * 聞こえる音のズレ（カーソル先行）が大きい。詰めて遅延を減らすが、下げすぎると
 * アンダーラン（音途切れ）になるため 150ms で妥協する。全画面共通で使う。
 */
const val AUDIO_BUFFER_MS: Double = 150.0

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
                if (v.isAttachedToWindow) {
                    runCatching { api.render() }
                }
            }
        }
    }
}

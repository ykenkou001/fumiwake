package dev.fumiwake.app.score

import alphaTab.AlphaTabView

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

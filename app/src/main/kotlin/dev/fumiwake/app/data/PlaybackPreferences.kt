package dev.fumiwake.app.data

import android.content.Context

/**
 * 再生オプション（メトロノーム／カウントイン）の永続化
 * （docs/METRONOME_PLAN.md §2.2）。
 *
 * テンポ・ループが譜例ごとにリセットされるのに対し、クリックは「常に鳴らしたい／
 * 常に切りたい」という性質の設定なので、エクササイズ切替やアプリ再起動をまたいで
 * 保持する。進捗（`:core` の `ProgressStore` 契約、SQLite 実装）とは関心が別であり、
 * 真偽値 2 つに SQLite は過剰なため [android.content.SharedPreferences] を使う。
 *
 * 分岐を持たない薄いラッパに保つ（JVM ユニットテストからは Android フレームワークに
 * 触れられないため、テストの要らない薄さを維持する — 同 §3）。
 */
class PlaybackPreferences(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * クリック（メトロノーム）を鳴らすか。既定 OFF。
     *
     * リズム領域が本アプリの最優先であることから当初は既定 ON にしたが、譜面の音に
     * クリックが加算されてヘッドルームを圧迫し、歪みの一因になりうるため既定を戻した
     * （docs/AUDIO_DEFECT_PLAN.md §3.2）。機能自体はそのままで、ON にすれば以後保持される。
     */
    var metronomeEnabled: Boolean
        get() = prefs.getBoolean(KEY_METRONOME, DEFAULT_METRONOME)
        set(value) = prefs.edit().putBoolean(KEY_METRONOME, value).apply()

    /** 再生前に予備カウントを入れるか。既定 OFF — 開始が遅れ「無音」と誤解されうるため。 */
    var countInEnabled: Boolean
        get() = prefs.getBoolean(KEY_COUNT_IN, DEFAULT_COUNT_IN)
        set(value) = prefs.edit().putBoolean(KEY_COUNT_IN, value).apply()

    /**
     * オーディオバッファ長（ミリ秒）。**音の途切れと再生カーソルのズレのトレードオフ点**
     * （docs/AUDIO_DEFECT_PLAN.md §29）。
     *
     * 大きいほど音が途切れにくいがカーソルが音より先行し、小さいほどカーソルは合うが
     * Bluetooth でうねり・途切れが出る。適切な値は出力先（スピーカー／Bluetooth）や
     * 機種によって変わり、こちらから決め打ちできないためユーザーが調整する。
     */
    var audioBufferMs: Int
        get() = prefs.getInt(KEY_AUDIO_BUFFER, DEFAULT_AUDIO_BUFFER_MS)
        set(value) = prefs.edit()
            .putInt(KEY_AUDIO_BUFFER, value.coerceIn(MIN_AUDIO_BUFFER_MS, MAX_AUDIO_BUFFER_MS))
            .apply()

    companion object {
        /** 下限。これ未満はアンダーランから復帰せず無音・カーソル凍結を招いた実績がある。 */
        const val MIN_AUDIO_BUFFER_MS = 300

        /** 上限。これ以上厚くしてもカーソルのズレが増えるだけで実用にならない。 */
        const val MAX_AUDIO_BUFFER_MS = 1500

        /** 既定。Bluetooth のうねりを抑えつつズレを抑える妥協点（実機で確認した値）。 */
        const val DEFAULT_AUDIO_BUFFER_MS = 700

        private const val PREFS_NAME = "playback_prefs"
        private const val KEY_METRONOME = "metronome_enabled"
        private const val KEY_COUNT_IN = "count_in_enabled"
        private const val KEY_AUDIO_BUFFER = "audio_buffer_ms"
        private const val DEFAULT_METRONOME = false
        private const val DEFAULT_COUNT_IN = false
    }
}

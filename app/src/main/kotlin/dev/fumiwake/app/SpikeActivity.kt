package dev.fumiwake.app

import alphaTab.AlphaTabView
import alphaTab.PlayerMode
import alphaTab.model.Beat
import alphaTab.model.Note
import alphaTab.synth.PlayerState
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import dev.fumiwake.app.score.AUDIO_BUFFER_MS
import dev.fumiwake.app.score.MASTER_VOLUME
import dev.fumiwake.app.score.renderWhenSized
import dev.fumiwake.core.music.NoteNames

/**
 * M2a Phase 0 spike screen, kept around as a debug entry point after M2b replaced
 * [MainActivity] with the full Compose UI (docs/IMPLEMENTATION_PLAN.md §6 M2b: "M2a の
 * スパイク画面は SpikeActivity として残し"). Reachable from the concept map's
 * "デバッグ: スパイク画面" link. Not exported (see AndroidManifest.xml).
 *
 * Purpose: prove on a real device that alphaTab 1.8.4 can render a hardcoded alphaTex
 * exercise, play it back with tempo/loop control, and report tap hit-testing (bar/beat/
 * fret/string/note name) — the go/no-go signal for the renderer choice. Not meant to be
 * pretty; see docs/IMPLEMENTATION_PLAN.md §5/§6 (M2a) and docs/alphatab-api-notes.md.
 *
 * Deliberately classic View + XML, no Compose: alphaTab.AlphaTabView only has a
 * (Context, AttributeSet) constructor, so XML inflate is the reliable construction path.
 * Uses androidx.activity.ComponentActivity (not AppCompatActivity) so the existing plain
 * android:theme in AndroidManifest.xml does not need to become a Theme.AppCompat theme.
 */
class SpikeActivity : ComponentActivity() {

    private var isLooping = false

    // onDestroy でプレイヤー／シンセを解放するために保持する。
    private var alphaTabView: AlphaTabView? = null

    // Hardcoded alphaTex, built from the verified syntax in docs/alphatab-api-notes.md:
    // notes are `fret.string.duration`, rests `r.duration`, dotted notes `{d}`, ties
    // `-.duration`, time signature `\ts <num> <den>`, bar lines `|`. 4 bars in 4/4,
    // containing exactly one dotted note, one tie, and one rest, tempo 70.
    private val spikeAlphaTex = """
        \title "M2a Spike" \tempo 70 . \ts 4 4
        0.6.4 2.6.4 3.6.4 0.5.4 |
        2.5.4 3.5.4{d} 0.4.8 2.4.4 |
        0.4.4 2.4.4 -.4 3.3.4 |
        r.4 0.3.4 2.3.4 3.3.4 |
    """.trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val alphaTabView = findViewById<AlphaTabView>(R.id.alphaTabView)
        this.alphaTabView = alphaTabView
        val btnPlayPause = findViewById<Button>(R.id.btnPlayPause)
        val btnTempo50 = findViewById<Button>(R.id.btnTempo50)
        val btnTempo75 = findViewById<Button>(R.id.btnTempo75)
        val btnTempo100 = findViewById<Button>(R.id.btnTempo100)
        val btnLoop = findViewById<Button>(R.id.btnLoop)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)

        // Playback buttons stay disabled until the player reports it is actually ready
        // (soundfont + midi loaded) — see api.playerReady below.
        btnPlayPause.isEnabled = false
        btnTempo50.isEnabled = false
        btnTempo75.isEnabled = false
        btnTempo100.isEnabled = false
        btnLoop.isEnabled = false

        // PlayerSettings.playerMode defaults to Disabled (confirmed via javap on the
        // PlayerSettings constructor) - playback must be opted into explicitly. The
        // bundled sonivox.sf2 soundfont and Bravura.otf music font ship inside the AAR's
        // assets/ and are picked up automatically; no manual loadSoundFont() call needed.
        val settings = alphaTabView.settings
        settings.player.enablePlayer = true
        settings.player.playerMode = PlayerMode.EnabledSynthesizer
        settings.player.enableCursor = true
        settings.player.enableUserInteraction = true
        // Bluetooth 出力時のうねり対策で既定より厚くする。エクササイズ画面と同じ方針。
        settings.player.bufferTimeInMilliseconds = AUDIO_BUFFER_MS
        alphaTabView.settings = settings
        alphaTabView.api.updateSettings()

        val api = alphaTabView.api
        // サステイン音のクリップ（音割れ）防止。エクササイズ画面と同じ設定。
        api.masterVolume = MASTER_VOLUME

        api.error.on { throwable ->
            runOnUiThread {
                tvStatus.text = "Error: ${throwable.message}"
            }
        }

        api.playerReady.on {
            runOnUiThread {
                btnPlayPause.isEnabled = true
                btnTempo50.isEnabled = true
                btnTempo75.isEnabled = true
                btnTempo100.isEnabled = true
                btnLoop.isEnabled = true
                tvStatus.text = "Ready. Tap a note to hear it and see its details."
            }
        }

        api.playerStateChanged.on { args ->
            runOnUiThread {
                btnPlayPause.text = if (args.state == PlayerState.Playing) {
                    getString(R.string.action_stop)
                } else {
                    getString(R.string.action_play)
                }
            }
        }

        // Tap hit-testing: alphaTab's own AndroidViewContainer already converts touch
        // coordinates on the render surface into boundsLookup hits internally (confirmed
        // via javap on alphaTab.platform.android.AndroidViewContainer /
        // AlphaTabRenderSurface) and republishes the result as these two api-level
        // events. Using them avoids re-deriving the touch -> canvas coordinate transform
        // (density, scroll offset) by hand, which is where the "known pitfalls" in the
        // task brief flagged the most risk.
        api.noteMouseDown.on { note: Note ->
            runOnUiThread {
                showNoteInfo(tvStatus, note)
                api.playNote(note)
            }
        }

        api.beatMouseDown.on { beat: Beat ->
            runOnUiThread {
                if (beat.isRest) {
                    showRestInfo(tvStatus, beat)
                }
            }
        }

        btnPlayPause.setOnClickListener {
            api.playPause()
        }
        btnTempo50.setOnClickListener {
            api.playbackSpeed = 0.5
            tvStatus.text = "Tempo: 50%"
        }
        btnTempo75.setOnClickListener {
            api.playbackSpeed = 0.75
            tvStatus.text = "Tempo: 75%"
        }
        btnTempo100.setOnClickListener {
            api.playbackSpeed = 1.0
            tvStatus.text = "Tempo: 100%"
        }
        btnLoop.setOnClickListener {
            isLooping = !isLooping
            api.isLooping = isLooping
            btnLoop.text = if (isLooping) getString(R.string.loop_on) else getString(R.string.loop_off)
        }

        tvStatus.text = "Loading score..."
        api.tex(spikeAlphaTex)

        // alphaTab 初期化競合（幅0でのスキップ後に再描画が発火しない）対策。
        // 詳細は dev.fumiwake.app.score.AlphaTabRenderFix.kt のコメント参照。
        alphaTabView.renderWhenSized()
    }

    override fun onDestroy() {
        // エクササイズ画面（ExerciseScoreView.onRelease）と方針を揃え、プレイヤーと
        // シンセ（オーディオスレッド等）を明示的に解放する。runCatching で
        // 未初期化／二重破棄を吸収。
        runCatching { alphaTabView?.api?.destroy() }
        alphaTabView = null
        super.onDestroy()
    }

    private fun showNoteInfo(tvStatus: TextView, note: Note) {
        val beat = note.beat
        val bar = beat.voice.bar
        val barIndex = bar.index.toInt()
        val beatIndex = beat.index.toInt()
        val fret = note.fret.toInt()
        val string = note.string.toInt()
        val noteName = NoteNames.fromMidi(note.realValue.toInt())
        tvStatus.text = "Bar $barIndex / Beat $beatIndex - fret $fret, string $string, note $noteName"
    }

    private fun showRestInfo(tvStatus: TextView, beat: Beat) {
        val bar = beat.voice.bar
        val barIndex = bar.index.toInt()
        val beatIndex = beat.index.toInt()
        tvStatus.text = "Bar $barIndex / Beat $beatIndex - rest"
    }
}

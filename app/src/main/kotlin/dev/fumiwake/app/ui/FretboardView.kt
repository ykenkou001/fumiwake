package dev.fumiwake.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.fumiwake.core.exercise.FretRange
import dev.fumiwake.core.music.FretPosition
import dev.fumiwake.core.music.Tuning

/**
 * A fretboard diagram (DESIGN.md §4.2). alphaTab draws the staff and the tab; the
 * fretboard is ours to draw.
 *
 * Strings run horizontally with the 1st string (highest sounding) on top — the way tab is
 * laid out, and the way a player looking down at their own guitar sees them.
 *
 * [highlighted] is where the tapped note is actually played; [alternatives] are the other
 * places that same pitch lives, derived rather than authored (DESIGN.md §8 Q2). Drawing
 * both at once is what makes `guitar.multi-positions` legible: one note in the score,
 * two or three places on the neck.
 *
 * Display only — no touch input. Answering "where is this note?" by tapping the diagram
 * belongs to the check mode in Phase 2-B.
 */
@Composable
fun FretboardView(
    fretRange: FretRange,
    highlighted: List<FretPosition>,
    alternatives: List<FretPosition>,
    modifier: Modifier = Modifier,
    stringCount: Int = Tuning.STANDARD.stringCount,
) {
    val stringColor = MaterialTheme.colorScheme.onSurfaceVariant
    val fretColor = MaterialTheme.colorScheme.outlineVariant
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(FRETBOARD_HEIGHT)
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            // 開放弦だけの譜例（fretRange 0..0）でも 1 コマぶんの幅を確保する。
            // ここが 0 になると割り算が壊れて図全体が潰れる。
            val fretCount = (fretRange.max - fretRange.min).coerceAtLeast(1)
            val lastFret = fretRange.min + fretCount

            // ナットの左に、開放弦の丸を置くための余白を取る。
            val nutMargin = size.width * NUT_MARGIN_FRACTION
            val fretWidth = (size.width - nutMargin) / fretCount

            // 丸が上下の端で欠けないよう、弦の並びを丸の半径ぶん内側に寄せる。
            val inset = DOT_RADIUS
            val stringGap = (size.height - inset * 2f) / (stringCount - 1).coerceAtLeast(1)

            /** 縦線（フレット）の x 座標。範囲の左端がナットの位置になる。 */
            fun fretLineX(fret: Int): Float = nutMargin + (fret - fretRange.min) * fretWidth

            /** 丸の x 座標。開放弦はナットの手前、押弦はフレット間の中央（コードダイアグラムと同じ作法）。 */
            fun dotX(fret: Int): Float =
                if (fret == 0) nutMargin / 2f else fretLineX(fret) - fretWidth / 2f

            fun stringY(string: Int): Float = inset + (string - 1) * stringGap

            val top = stringY(1)
            val bottom = stringY(stringCount)

            for (fret in fretRange.min..lastFret) {
                // 0フレット（ナット）だけは太く描く。ここが「開放弦の位置」の目印になる。
                val isNut = fret == 0
                drawLine(
                    color = if (isNut) stringColor else fretColor,
                    start = Offset(fretLineX(fret), top),
                    end = Offset(fretLineX(fret), bottom),
                    strokeWidth = if (isNut) NUT_STROKE else FRET_STROKE,
                )
            }

            // ポジションマーク。実物の指板と同じ位置に置くと、図と現物が結びつく。
            for (fret in POSITION_MARKS) {
                if (fret <= fretRange.min || fret > lastFret) continue
                drawCircle(
                    color = fretColor,
                    radius = MARKER_RADIUS,
                    center = Offset(dotX(fret), (top + bottom) / 2f),
                )
            }

            for (string in 1..stringCount) {
                drawLine(
                    color = stringColor,
                    start = Offset(0f, stringY(string)),
                    end = Offset(size.width, stringY(string)),
                    strokeWidth = STRING_STROKE,
                )
            }

            // 代替位置を先に描き、実際に鳴る位置を上に重ねる（重なったとき主が勝つ）。
            for (position in alternatives) {
                drawPosition(dotX(position.fret), stringY(position.string), secondary, filled = false)
            }
            for (position in highlighted) {
                drawPosition(dotX(position.fret), stringY(position.string), primary, filled = true)
            }
        }

        if (alternatives.isNotEmpty()) {
            Text(
                text = "同じ音が他に${alternatives.size}箇所あります（白抜きの丸）",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

private fun DrawScope.drawPosition(x: Float, y: Float, color: Color, filled: Boolean) {
    if (filled) {
        drawCircle(color = color, radius = DOT_RADIUS, center = Offset(x, y))
    } else {
        drawCircle(color = color, radius = DOT_RADIUS, center = Offset(x, y), style = DOT_OUTLINE)
    }
}

private val FRETBOARD_HEIGHT = 120.dp
private const val NUT_MARGIN_FRACTION = 0.08f
private const val NUT_STROKE = 6f
private const val FRET_STROKE = 2f
private const val STRING_STROKE = 2f
private const val MARKER_RADIUS = 6f
private const val DOT_RADIUS = 14f
private val DOT_OUTLINE = Stroke(width = 3f)
private val POSITION_MARKS = listOf(3, 5, 7, 9, 12)

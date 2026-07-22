package dev.fumiwake.core.music

/**
 * `(duration value, dots, isRest)` -> Japanese display text, e.g. "4分音符",
 * "付点4分音符", "4分休符".
 *
 * [durationValue] follows alphaTab's `Duration.value` convention
 * (1=whole, 2=half, 4=quarter, 8=eighth, 16=sixteenth, 32=thirty-second).
 * `:core` cannot depend on alphaTab, so this takes a plain Int; `:app` passes
 * `Duration.value` through.
 */
object DurationTexts {

    private val VALUE_NAMES = mapOf(
        1 to "全",
        2 to "2分",
        4 to "4分",
        8 to "8分",
        16 to "16分",
        32 to "32分",
    )

    fun of(durationValue: Int, dots: Int, isRest: Boolean = false): String {
        val base = VALUE_NAMES[durationValue]
            ?: throw IllegalArgumentException("unsupported duration value: $durationValue")
        val dotPrefix = if (dots > 0) "付点" else ""
        val noun = if (isRest) "休符" else "音符"
        return "$dotPrefix$base$noun"
    }
}

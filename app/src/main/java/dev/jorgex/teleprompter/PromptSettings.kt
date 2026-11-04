package dev.jorgex.teleprompter

enum class PromptPosition { TOP, CENTER }

data class PromptSettings(
    val speed: Int = 6,
    val textSizeSp: Int = 24,
    val lines: Int = 4,
    val textColor: Int = COLOR_WHITE,
    val backgroundColor: Int = COLOR_BLACK,
    val backgroundOpacity: Int = 60,
    val position: PromptPosition = PromptPosition.TOP,
    val startDelaySeconds: Int = 0,
) {
    companion object {
        const val MIN_SPEED = 1
        const val MAX_SPEED = 10
        const val MIN_TEXT_SIZE = 14
        const val MAX_TEXT_SIZE = 48
        const val MIN_LINES = 2
        const val MAX_LINES = 8
        const val MIN_DELAY = 0
        const val MAX_DELAY = 10

        val COLOR_WHITE = 0xFFFFFFFF.toInt()
        val COLOR_YELLOW = 0xFFFFEB3B.toInt()
        val COLOR_GREEN = 0xFF69F0AE.toInt()
        val COLOR_CYAN = 0xFF80D8FF.toInt()
        val COLOR_BLACK = 0xFF000000.toInt()
        val COLOR_GRAY = 0xFF242424.toInt()
    }
}

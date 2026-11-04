package dev.jorgex.teleprompter

import android.content.Context

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("teleprompter", Context.MODE_PRIVATE)

    fun load(): PromptSettings = PromptSettings(
        speed = prefs.getInt(KEY_SPEED, 6),
        textSizeSp = prefs.getInt(KEY_TEXT_SIZE, 24),
        lines = prefs.getInt(KEY_LINES, 4),
        textColor = prefs.getInt(KEY_TEXT_COLOR, PromptSettings.COLOR_WHITE),
        backgroundColor = prefs.getInt(KEY_BACKGROUND, PromptSettings.COLOR_BLACK),
        backgroundOpacity = prefs.getInt(KEY_OPACITY, 60),
        position = if (prefs.getString(KEY_POSITION, null) == PromptPosition.CENTER.name) {
            PromptPosition.CENTER
        } else {
            PromptPosition.TOP
        },
        startDelaySeconds = prefs.getInt(KEY_DELAY, 0),
    )

    fun save(settings: PromptSettings) {
        prefs.edit()
            .putInt(KEY_SPEED, settings.speed)
            .putInt(KEY_TEXT_SIZE, settings.textSizeSp)
            .putInt(KEY_LINES, settings.lines)
            .putInt(KEY_TEXT_COLOR, settings.textColor)
            .putInt(KEY_BACKGROUND, settings.backgroundColor)
            .putInt(KEY_OPACITY, settings.backgroundOpacity)
            .putString(KEY_POSITION, settings.position.name)
            .putInt(KEY_DELAY, settings.startDelaySeconds)
            .apply()
    }

    /** Prevents re-creating the demo script if the user deleted it. */
    var demoSeeded: Boolean
        get() = prefs.getBoolean(KEY_DEMO_SEEDED, false)
        set(value) = prefs.edit().putBoolean(KEY_DEMO_SEEDED, value).apply()

    private companion object {
        const val KEY_SPEED = "speed"
        const val KEY_TEXT_SIZE = "text_size"
        const val KEY_LINES = "lines"
        const val KEY_TEXT_COLOR = "text_color"
        const val KEY_BACKGROUND = "background"
        const val KEY_OPACITY = "opacity"
        const val KEY_POSITION = "position"
        const val KEY_DELAY = "delay"
        const val KEY_DEMO_SEEDED = "demo_seeded"
    }
}

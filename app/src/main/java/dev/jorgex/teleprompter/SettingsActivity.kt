package dev.jorgex.teleprompter

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView

class SettingsActivity : Activity() {

    private lateinit var store: SettingsStore
    private var settings = PromptSettings()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        store = SettingsStore(this)
        settings = store.load()

        findViewById<Button>(R.id.btnClose).setOnClickListener { finish() }

        bindSeek(
            seekId = R.id.speedSeek,
            valueId = R.id.speedValue,
            min = PromptSettings.MIN_SPEED,
            max = PromptSettings.MAX_SPEED,
            get = { it.speed },
            label = { "$it / ${PromptSettings.MAX_SPEED}" },
            update = { current, value -> current.copy(speed = value) },
        )

        bindSeek(
            seekId = R.id.sizeSeek,
            valueId = R.id.sizeValue,
            min = PromptSettings.MIN_TEXT_SIZE,
            max = PromptSettings.MAX_TEXT_SIZE,
            get = { it.textSizeSp },
            label = { "$it sp" },
            update = { current, value -> current.copy(textSizeSp = value) },
        )

        bindSeek(
            seekId = R.id.linesSeek,
            valueId = R.id.linesValue,
            min = PromptSettings.MIN_LINES,
            max = PromptSettings.MAX_LINES,
            get = { it.lines },
            label = { "$it lines" },
            update = { current, value -> current.copy(lines = value) },
        )

        bindSeek(
            seekId = R.id.alphaSeek,
            valueId = R.id.alphaValue,
            min = 0,
            max = 100,
            get = { it.backgroundOpacity },
            label = { "$it %" },
            update = { current, value -> current.copy(backgroundOpacity = value) },
        )

        bindSeek(
            seekId = R.id.delaySeek,
            valueId = R.id.delayValue,
            min = PromptSettings.MIN_DELAY,
            max = PromptSettings.MAX_DELAY,
            get = { it.startDelaySeconds },
            label = { if (it == 0) "No delay" else "$it s" },
            update = { current, value -> current.copy(startDelaySeconds = value) },
        )

        bindTextColor()
        bindBackgroundColor()
        bindPosition()
    }

    private fun bindSeek(
        seekId: Int,
        valueId: Int,
        min: Int,
        max: Int,
        get: (PromptSettings) -> Int,
        label: (Int) -> String,
        update: (PromptSettings, Int) -> PromptSettings,
    ) {
        val seekBar = findViewById<SeekBar>(seekId)
        val valueView = findViewById<TextView>(valueId)
        seekBar.max = max - min
        seekBar.progress = get(settings) - min
        valueView.text = label(get(settings))
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = min + progress
                valueView.text = label(value)
                settings = update(settings, value)
                store.save(settings)
            }

            override fun onStartTrackingTouch(bar: SeekBar?) = Unit
            override fun onStopTrackingTouch(bar: SeekBar?) = Unit
        })
    }

    private fun bindTextColor() {
        val group = findViewById<RadioGroup>(R.id.textColorGroup)
        group.check(
            when (settings.textColor) {
                PromptSettings.COLOR_YELLOW -> R.id.colorYellow
                PromptSettings.COLOR_GREEN -> R.id.colorGreen
                PromptSettings.COLOR_CYAN -> R.id.colorCyan
                else -> R.id.colorWhite
            }
        )
        group.setOnCheckedChangeListener { _, checkedId ->
            val color = when (checkedId) {
                R.id.colorYellow -> PromptSettings.COLOR_YELLOW
                R.id.colorGreen -> PromptSettings.COLOR_GREEN
                R.id.colorCyan -> PromptSettings.COLOR_CYAN
                else -> PromptSettings.COLOR_WHITE
            }
            settings = settings.copy(textColor = color)
            store.save(settings)
        }
    }

    private fun bindBackgroundColor() {
        val group = findViewById<RadioGroup>(R.id.bgColorGroup)
        group.check(
            when (settings.backgroundColor) {
                PromptSettings.COLOR_GRAY -> R.id.bgGray
                PromptSettings.COLOR_WHITE -> R.id.bgWhite
                else -> R.id.bgBlack
            }
        )
        group.setOnCheckedChangeListener { _, checkedId ->
            val color = when (checkedId) {
                R.id.bgGray -> PromptSettings.COLOR_GRAY
                R.id.bgWhite -> PromptSettings.COLOR_WHITE
                else -> PromptSettings.COLOR_BLACK
            }
            settings = settings.copy(backgroundColor = color)
            store.save(settings)
        }
    }

    private fun bindPosition() {
        val group = findViewById<RadioGroup>(R.id.positionGroup)
        group.check(if (settings.position == PromptPosition.TOP) R.id.posTop else R.id.posCenter)
        group.setOnCheckedChangeListener { _, checkedId ->
            val position = if (checkedId == R.id.posCenter) PromptPosition.CENTER else PromptPosition.TOP
            settings = settings.copy(position = position)
            store.save(settings)
        }
    }
}

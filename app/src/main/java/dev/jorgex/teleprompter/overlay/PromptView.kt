package dev.jorgex.teleprompter.overlay

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.util.TypedValue
import android.view.Choreographer
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.TextView
import dev.jorgex.teleprompter.PromptSettings
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * The strip itself: draws the text, scrolls it at the configured speed and
 * translates the gestures.
 *
 * Gestures:
 *  - short tap: pause or resume (pause stays fixed).
 *  - drag: moves the script 1:1 with the finger; on release it returns to the previous state.
 *  - double tap: closes the teleprompter.
 */
class PromptView(
    context: Context,
    text: String,
    private val settings: PromptSettings,
    private val onRequestClose: () -> Unit,
) : FrameLayout(context) {

    private val content = TextView(context)
    private val shadowColor = 0xCC000000.toInt()

    private val verticalPadding = dp(10f)
    private val horizontalPadding = dp(14f)

    /** Height the window needs to show exactly the configured lines. */
    val requiredHeight: Int

    private var maxOffset = 0f

    var offset: Float = 0f
        private set

    private var playing = false
    private var lastFrameNanos = 0L
    private var pendingStartAt = 0L

    private var downRawY = 0f
    private var downTime = 0L
    private var downOffset = 0f
    private var dragging = false
    private var wasPlayingBeforeTouch = false
    private var lastTapAt = 0L

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val speedPxPerSecond =
        settings.speed * DP_PER_SPEED_STEP * resources.displayMetrics.density

    init {
        setPadding(
            horizontalPadding.toInt(),
            verticalPadding.toInt(),
            horizontalPadding.toInt(),
            verticalPadding.toInt(),
        )
        background = GradientDrawable().apply {
            cornerRadius = dp(14f)
            setColor(withOpacity(settings.backgroundColor, settings.backgroundOpacity))
        }
        clipChildren = true
        clipToPadding = true

        content.apply {
            setTextColor(settings.textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, settings.textSizeSp.toFloat())
            setLineSpacing(0f, LINE_SPACING)
            includeFontPadding = false
            setShadowLayer(dp(3f), 0f, dp(1f), shadowColor)
            // Space below the last line so it does not stick to the edge when finished.
            setPadding(0, 0, 0, dp(8f).toInt())
            this.text = text
        }
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val metrics = content.paint.fontMetrics
        val lineHeight = (metrics.descent - metrics.ascent) * LINE_SPACING
        requiredHeight = (lineHeight * settings.lines + verticalPadding * 2f).toInt() + 1
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        post {
            val visible = height - paddingTop - paddingBottom
            maxOffset = max(0f, content.height - visible.toFloat())
            applyOffset(offset)
        }
    }

    /**
     * FrameLayout would measure the text with a maximum height equal to the
     * strip and clip it to that height, so we measure it without a limit: the
     * strip clips visually, but the text keeps its full height so it can scroll.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val availableWidth = measuredWidth - paddingLeft - paddingRight
        if (availableWidth <= 0) return
        content.measure(
            MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawY = event.rawY
                downTime = event.eventTime
                downOffset = offset
                dragging = false
                wasPlayingBeforeTouch = playing || pendingStartAt > 0L
                pause()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val delta = event.rawY - downRawY
                if (!dragging && abs(delta) > touchSlop) {
                    dragging = true
                    lastTapAt = 0L
                }
                if (dragging) applyOffset(downOffset - delta)
                return true
            }

            MotionEvent.ACTION_UP -> {
                val duration = event.eventTime - downTime
                val traveled = abs(event.rawY - downRawY)
                val isTap = !dragging && duration <= TAP_TIMEOUT_MS && traveled < touchSlop
                if (isTap) {
                    val now = SystemClock.uptimeMillis()
                    if (lastTapAt > 0L && now - lastTapAt <= DOUBLE_TAP_MS) {
                        lastTapAt = 0L
                        pause()
                        onRequestClose()
                    } else {
                        lastTapAt = now
                        // It was stopped before the touch: the tap means play.
                        if (!wasPlayingBeforeTouch) play()
                    }
                } else if (wasPlayingBeforeTouch) {
                    play()
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                if (wasPlayingBeforeTouch) play()
                return true
            }
        }
        return true
    }

    fun restoreOffset(value: Float) {
        applyOffset(value)
    }

    private fun play() {
        if (offset >= maxOffset) return
        playing = true
        lastFrameNanos = 0L
        pendingStartAt = if (settings.startDelaySeconds > 0) {
            SystemClock.uptimeMillis() + settings.startDelaySeconds * 1000L
        } else {
            0L
        }
        val choreographer = Choreographer.getInstance()
        choreographer.removeFrameCallback(frameCallback)
        choreographer.postFrameCallback(frameCallback)
    }

    private fun pause() {
        playing = false
        pendingStartAt = 0L
        lastFrameNanos = 0L
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    private fun applyOffset(value: Float) {
        offset = min(max(0f, value), maxOffset)
        content.translationY = -offset
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!playing) return

            if (pendingStartAt > 0L) {
                if (SystemClock.uptimeMillis() < pendingStartAt) {
                    lastFrameNanos = 0L
                    Choreographer.getInstance().postFrameCallback(this)
                    return
                }
                pendingStartAt = 0L
            }

            if (lastFrameNanos == 0L) {
                // First frame after a pause or delay: we only take the reference.
                lastFrameNanos = frameTimeNanos
                Choreographer.getInstance().postFrameCallback(this)
                return
            }

            val deltaSeconds = ((frameTimeNanos - lastFrameNanos) / 1_000_000_000.0)
                .toFloat()
                .coerceIn(0f, MAX_FRAME_SECONDS)
            lastFrameNanos = frameTimeNanos

            applyOffset(offset + speedPxPerSecond * deltaSeconds)
            if (offset >= maxOffset) {
                // End of the script: it stays still with the last line visible.
                pause()
                return
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun withOpacity(color: Int, percent: Int): Int {
        val alpha = (255 * percent / 100f).toInt().coerceIn(0, 255)
        return (alpha shl 24) or (color and 0x00FFFFFF)
    }

    private companion object {
        const val LINE_SPACING = 1.18f
        const val DP_PER_SPEED_STEP = 3.5f
        const val TAP_TIMEOUT_MS = 300L
        const val DOUBLE_TAP_MS = 350L
        const val MAX_FRAME_SECONDS = 0.1f
    }
}

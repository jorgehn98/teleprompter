package dev.jorgex.teleprompter.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowManager
import dev.jorgex.teleprompter.PromptPosition
import dev.jorgex.teleprompter.PromptSettings

/**
 * Floating window containing the teleprompter strip. It only occupies its own
 * height: touches outside it go to the app underneath.
 */
class PromptOverlay(
    private val context: Context,
    private val text: String,
    private val settings: PromptSettings,
    private val onRequestClose: () -> Unit,
) {

    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var view: PromptView? = null

    fun show(restoreOffset: Float = 0f) {
        if (view != null) return
        val promptView = PromptView(context, text, settings, onRequestClose)
        windowManager.addView(promptView, buildParams(promptView.requiredHeight))
        view = promptView
        if (restoreOffset > 0f) promptView.post { promptView.restoreOffset(restoreOffset) }
    }

    fun hide() {
        view?.let { promptView -> runCatching { windowManager.removeView(promptView) } }
        view = null
    }

    fun offsetNow(): Float = view?.offset ?: 0f

    private fun buildParams(heightPx: Int): WindowManager.LayoutParams {
        val metrics = context.resources.displayMetrics
        val margin = (MARGIN_DP * metrics.density).toInt()
        val params = WindowManager.LayoutParams(
            metrics.widthPixels - margin * 2,
            heightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )
        params.x = margin
        params.title = "Teleprompter"
        if (settings.position == PromptPosition.TOP) {
            params.gravity = Gravity.TOP or Gravity.START
            // Extra gap below the status bar so the camera app's top controls
            // (flash, timer, settings) are not covered.
            params.y = statusBarInset() + (TOP_GAP_DP * metrics.density).toInt()
        } else {
            params.gravity = Gravity.CENTER_VERTICAL or Gravity.START
        }
        return params
    }

    private fun statusBarInset(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insets = windowManager.currentWindowMetrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.statusBars() or WindowInsets.Type.displayCutout()
            )
            return insets.top
        }
        val id = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) context.resources.getDimensionPixelSize(id) else 0
    }

    private companion object {
        const val MARGIN_DP = 12f
        const val TOP_GAP_DP = 34f
    }
}

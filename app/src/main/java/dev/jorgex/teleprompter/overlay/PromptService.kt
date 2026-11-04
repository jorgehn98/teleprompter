package dev.jorgex.teleprompter.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import dev.jorgex.teleprompter.R
import dev.jorgex.teleprompter.ScriptListActivity
import dev.jorgex.teleprompter.SettingsStore

/**
 * Keeps the strip alive while the user records with the camera app, which will
 * be in the foreground. It is a foreground service so the system does not take
 * down the floating window.
 */
class PromptService : Service() {

    private var overlay: PromptOverlay? = null
    private var currentText = ""
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> shutdown()
            ACTION_START -> {
                val text = intent.getStringExtra(EXTRA_TEXT)
                if (text.isNullOrBlank()) {
                    shutdown()
                } else {
                    currentText = text
                    startForegroundCompat()
                    showOverlay(0f)
                }
            }
        }
        return START_NOT_STICKY
    }

    /** The window is rebuilt when the device rotates, preserving the script position. */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val position = overlay?.offsetNow() ?: return
        overlay?.hide()
        overlay = null
        showOverlay(position)
    }

    override fun onDestroy() {
        overlay?.hide()
        overlay = null
        isRunning = false
        super.onDestroy()
    }

    private fun showOverlay(restoreOffset: Float) {
        overlay?.hide()
        val settings = SettingsStore(this).load()
        val prompt = PromptOverlay(this, currentText, settings) {
            // The close comes from the gesture: leave the touch dispatch before removing the window.
            mainHandler.post { shutdown() }
        }
        try {
            prompt.show(restoreOffset)
            overlay = prompt
            isRunning = true
        } catch (throwable: Throwable) {
            Log.e(TAG, "Could not show the strip", throwable)
            shutdown()
        }
    }

    private fun shutdown() {
        overlay?.hide()
        overlay = null
        isRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundCompat() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, ScriptListActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, PromptService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopAction = Notification.Action.Builder(
            R.drawable.ic_stat_prompt,
            getString(R.string.notification_stop),
            stop,
        ).build()

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_prompt)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(open)
            .addAction(stopAction)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { setShowBadge(false) }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "PromptService"
        private const val ACTION_START = "dev.jorgex.teleprompter.action.START"
        private const val ACTION_STOP = "dev.jorgex.teleprompter.action.STOP"
        private const val EXTRA_TEXT = "text"
        private const val CHANNEL_ID = "prompt_active"
        private const val NOTIFICATION_ID = 41

        /** The app and the service live in the same process: used to paint the state in the UI. */
        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context, text: String) {
            val intent = Intent(context, PromptService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TEXT, text)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, PromptService::class.java).setAction(ACTION_STOP)
            )
        }
    }
}

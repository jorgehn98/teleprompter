package dev.jorgex.teleprompter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings

object OverlayPermission {

    fun isGranted(context: Context): Boolean = AndroidSettings.canDrawOverlays(context)

    fun request(activity: Activity) {
        val intent = Intent(
            AndroidSettings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${activity.packageName}"),
        )
        activity.startActivity(intent)
    }
}

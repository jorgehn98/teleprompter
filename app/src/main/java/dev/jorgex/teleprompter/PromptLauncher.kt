package dev.jorgex.teleprompter

import android.app.Activity
import android.app.AlertDialog
import android.widget.Toast
import dev.jorgex.teleprompter.overlay.PromptService

/**
 * Single entry point to the teleprompter: checks the overlay permission,
 * starts the service and sends the app to the background so the strip stays
 * visible over whatever is underneath (the camera).
 */
object PromptLauncher {

    fun start(activity: Activity, text: String) {
        if (text.isBlank()) {
            Toast.makeText(activity, "The script is empty.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!OverlayPermission.isGranted(activity)) {
            AlertDialog.Builder(activity)
                .setTitle("Permission required")
                .setMessage(
                    "To show the script over the camera, Teleprompter needs permission to appear " +
                        "on top of other apps.\n\n" +
                        "Settings → Apps → Special access → Appear on top."
                )
                .setPositiveButton("Open settings") { _, _ -> OverlayPermission.request(activity) }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        PromptService.start(activity, text)
        activity.moveTaskToBack(true)
    }
}

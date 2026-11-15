package com.astroframe.tv.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.astroframe.tv.data.PreferencesManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("BootReceiver", "Received broadcast: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val prefs = PreferencesManager(context)
            ApodWallpaperWorker.schedulePeriodic(
                context = context,
                intervalMinutes = prefs.intervalMinutes,
                forceReplace = true
            )
            Log.d("BootReceiver", "Re-enqueued AstroFrame background wallpaper worker after boot/update")
        }
    }
}


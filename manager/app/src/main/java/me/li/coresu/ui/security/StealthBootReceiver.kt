package me.li.coresu.ui.security

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StealthBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED) return
        Stealth.ensureLauncherVisible(context)
    }
}

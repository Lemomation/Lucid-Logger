package com.lemomation.lucidlogger.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lemomation.lucidlogger.appContainer
import com.lemomation.lucidlogger.data.prefs.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in SUPPORTED_ACTIONS) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val alarms = context.appContainer().alarms.snapshot()
                AlarmScheduler(context).scheduleAll(alarms)
                val settings = SettingsRepository(context).current()
                MorningReminderScheduler(context).schedule(settings)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}

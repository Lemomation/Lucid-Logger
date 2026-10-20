package com.lemomation.lucidlogger.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.lemomation.lucidlogger.appContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ALARM) return
        val id = intent.getStringExtra(EXTRA_ALARM_ID) ?: return
        val isSnooze = intent.getBooleanExtra(EXTRA_SNOOZE, false)
        val alarm = context.appContainer().alarms.get(id) ?: return
        if (!alarm.enabled && !isSnooze) return

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START
            putExtra(AlarmService.EXTRA_ALARM_ID, alarm.id)
        }
        runCatching { ContextCompat.startForegroundService(context, serviceIntent) }
        runCatching {
            context.startActivity(
                Intent(context, AlarmRingActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(AlarmRingActivity.EXTRA_ALARM_ID, alarm.id)
                },
            )
        }
        if (!isSnooze) {
            if (alarm.days.isEmpty()) {
                val pending = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        context.appContainer().alarms.setEnabled(alarm.id, false)
                        AlarmScheduler(context).cancel(alarm)
                    } finally {
                        pending.finish()
                    }
                }
            } else {
                runCatching { AlarmScheduler(context).schedule(alarm) }
            }
        }
    }

    companion object {
        const val ACTION_ALARM = "com.lemomation.lucidlogger.action.ALARM"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_SNOOZE = "snooze"
    }
}

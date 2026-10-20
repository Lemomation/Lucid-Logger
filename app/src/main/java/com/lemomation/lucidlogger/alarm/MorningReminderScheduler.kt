package com.lemomation.lucidlogger.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.lemomation.lucidlogger.data.model.AppSettings
import java.time.ZonedDateTime

class MorningReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(settings: AppSettings) {
        val pendingIntent = pendingIntent()
        if (!settings.morningReminder) {
            alarmManager.cancel(pendingIntent)
            return
        }
        val now = ZonedDateTime.now()
        var next = now
            .withHour(settings.morningReminderHour)
            .withMinute(settings.morningReminderMinute)
            .withSecond(0)
            .withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        runCatching {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                next.toInstant().toEpochMilli(),
                pendingIntent,
            )
        }
    }

    fun cancel() {
        alarmManager.cancel(pendingIntent())
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        5001,
        Intent(context, MorningReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

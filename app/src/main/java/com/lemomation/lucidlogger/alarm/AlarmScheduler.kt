package com.lemomation.lucidlogger.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.lemomation.lucidlogger.MainActivity
import com.lemomation.lucidlogger.data.model.Alarm
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true

    fun schedule(alarm: Alarm) {
        if (!alarm.enabled) {
            cancel(alarm)
            return
        }
        val trigger = nextTrigger(alarm) ?: return
        val showIntent = PendingIntent.getActivity(
            context,
            alarm.id.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val alarmIntent = alarmPendingIntent(alarm, requestCode = alarm.id.hashCode())
        runCatching {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(trigger.toInstant().toEpochMilli(), showIntent),
                alarmIntent,
            )
        }.onFailure {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                trigger.toInstant().toEpochMilli(),
                alarmIntent,
            )
        }
    }

    fun scheduleAll(alarms: List<Alarm>) {
        alarms.forEach { schedule(it) }
    }

    fun cancel(alarm: Alarm) {
        alarmManager.cancel(alarmPendingIntent(alarm, alarm.id.hashCode()))
    }

    fun scheduleSnooze(alarm: Alarm, minutes: Int) {
        val trigger = ZonedDateTime.now().plusMinutes(minutes.toLong())
        val showIntent = PendingIntent.getActivity(
            context,
            alarm.id.hashCode() + 1,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val alarmIntent = alarmPendingIntent(alarm, requestCode = alarm.id.hashCode() + 7919, snooze = true)
        runCatching {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(trigger.toInstant().toEpochMilli(), showIntent),
                alarmIntent,
            )
        }.onFailure {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                trigger.toInstant().toEpochMilli(),
                alarmIntent,
            )
        }
    }

    fun nextTrigger(alarm: Alarm, from: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime? {
        for (offset in 0..8) {
            val date = from.toLocalDate().plusDays(offset.toLong())
            if (alarm.days.isNotEmpty() && date.dayOfWeek.value !in alarm.days) continue
            val candidate = ZonedDateTime.of(date, LocalTime.of(alarm.hour, alarm.minute), ZoneId.systemDefault())
            if (candidate.isAfter(from.plusSeconds(2))) return candidate
        }
        return null
    }

    private fun alarmPendingIntent(alarm: Alarm, requestCode: Int, snooze: Boolean = false): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_ALARM
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
                putExtra(AlarmReceiver.EXTRA_SNOOZE, snooze)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}

package com.lemomation.lucidlogger.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lemomation.lucidlogger.MainActivity
import com.lemomation.lucidlogger.R
import com.lemomation.lucidlogger.data.model.DreamSource
import com.lemomation.lucidlogger.data.prefs.SettingsRepository
import com.lemomation.lucidlogger.data.store.DreamStore
import com.lemomation.lucidlogger.util.TextUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

class MorningReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = SettingsRepository(context).current()
                if (settings.morningReminder) {
                    maybeNotify(context)
                }
                MorningReminderScheduler(context).schedule(settings)
            } finally {
                pending.finish()
            }
        }
    }

    private fun maybeNotify(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        createChannel(context)

        val cutoff = LocalDate.now().minusDays(3)
        val pendingNotes = DreamStore(context).list().filter {
            !it.date.isBefore(cutoff) &&
                it.source == DreamSource.VOICE &&
                !TextUtils.hasWrittenText(it.body)
        }
        if (pendingNotes.isEmpty()) return

        val openIntent = PendingIntent.getActivity(
            context,
            5101,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setColor(0xFFF59E0B.toInt())
            .setContentTitle("Dreams still need their morning note")
            .setContentText(
                if (pendingNotes.size == 1) "1 voice memo from last night is waiting for details."
                else "${pendingNotes.size} voice memos from last night are waiting for details.",
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(NOTIFICATION_ID, notification) }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.morning_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.morning_channel_description)
                enableVibration(false)
            },
        )
    }

    private companion object {
        const val CHANNEL_ID = "morning_review"
        const val NOTIFICATION_ID = 5102
    }
}

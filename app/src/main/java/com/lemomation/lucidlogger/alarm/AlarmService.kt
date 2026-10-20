package com.lemomation.lucidlogger.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.lemomation.lucidlogger.R
import com.lemomation.lucidlogger.audio.BuiltinSoundCatalog
import com.lemomation.lucidlogger.appContainer
import com.lemomation.lucidlogger.data.model.Alarm
import com.lemomation.lucidlogger.data.model.AppSettings
import com.lemomation.lucidlogger.data.model.BuiltinSounds
import com.lemomation.lucidlogger.data.prefs.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AlarmService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var focusRequest: AudioFocusRequest? = null
    private var rampJob: Job? = null
    private var alarmId: String? = null
    private var paused = false

    override fun onCreate() {
        super.onCreate()
        createAlarmChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val id = intent.getStringExtra(EXTRA_ALARM_ID)
                if (id.isNullOrBlank()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                alarmId = id
                paused = false
                startForegroundNow(id)
                scope.launch { beginRinging(id) }
            }
            ACTION_PAUSE -> pauseRinging()
            ACTION_RESUME -> resumeRinging()
            ACTION_STOP -> {
                stopAll()
                stopSelf()
            }
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private suspend fun beginRinging(id: String) {
        val alarm = applicationContext.appContainer().alarms.get(id) ?: run {
            stopSelf()
            return
        }
        val settings = SettingsRepository(this).current()
        acquireWakeLock()
        startSound(alarm, settings)
        if (paused) runCatching { player?.pause() }
        if (alarm.vibrate && !paused) startVibration(settings.vibrationPattern)
    }

    private fun startSound(alarm: Alarm, settings: AppSettings) {
        stopSound()
        val attrs = audioAttributes
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener { }
            .build()
        focusRequest = request
        runCatching {
            (getSystemService(Context.AUDIO_SERVICE) as AudioManager).requestAudioFocus(request)
        }

        val created = runCatching {
            if (alarm.soundId == BuiltinSounds.CUSTOM && !alarm.customSoundUri.isNullOrBlank()) {
                MediaPlayer().apply {
                    setAudioAttributes(attrs)
                    setDataSource(this@AlarmService, Uri.parse(alarm.customSoundUri))
                    isLooping = true
                    prepare()
                }
            } else {
                MediaPlayer.create(this, BuiltinSoundCatalog.resId(alarm.soundId)).apply {
                    setAudioAttributes(attrs)
                    isLooping = true
                }
            }
        }.getOrElse {
            MediaPlayer.create(this, R.raw.alarm_singing_bowl).apply {
                setAudioAttributes(attrs)
                isLooping = true
            }
        }

        player = created
        created.setVolume(0f, 0f)
        runCatching { created.start() }
        val target = settings.alarmVolume.coerceIn(0.05f, 1f)
        val rampSeconds = alarm.volumeRampSeconds
        if (rampSeconds <= 0) {
            created.setVolume(target, target)
        } else {
            rampJob?.cancel()
            rampJob = scope.launch {
                val steps = (rampSeconds * 10).coerceAtLeast(1)
                repeat(steps) { index ->
                    if (!isActive) return@launch
                    val volume = target * ((index + 1f) / steps)
                    player?.setVolume(volume, volume)
                    delay(100)
                }
            }
        }
    }

    private fun startVibration(pattern: String) {
        val vibrator = getVibrator()
        this.vibrator = vibrator
        val timings = when (pattern) {
            "heartbeat" -> longArrayOf(0, 180, 120, 220, 900)
            "strong" -> longArrayOf(0, 800, 300)
            "off" -> return
            else -> longArrayOf(0, 350, 650, 350, 1400)
        }
        runCatching {
            vibrator.vibrate(VibrationEffect.createWaveform(timings, 0))
        }
    }

    private fun getVibrator(): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    private fun pauseRinging() {
        paused = true
        rampJob?.cancel()
        runCatching { player?.pause() }
        runCatching { vibrator?.cancel() }
    }

    private fun resumeRinging() {
        paused = false
        runCatching { player?.start() }
        scope.launch {
            val settings = SettingsRepository(this@AlarmService).current()
            val alarm = alarmId?.let { applicationContext.appContainer().alarms.get(it) }
            if (alarm?.vibrate == true) startVibration(settings.vibrationPattern)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val power = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LucidLogger:AlarmRing").apply {
            setReferenceCounted(false)
            acquire(60 * 60 * 1000L)
        }
    }

    private fun stopSound() {
        rampJob?.cancel()
        rampJob = null
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
    }

    private fun stopAll() {
        paused = false
        stopSound()
        runCatching { vibrator?.cancel() }
        vibrator = null
        focusRequest?.let { request ->
            runCatching {
                (getSystemService(Context.AUDIO_SERVICE) as AudioManager).abandonAudioFocusRequest(request)
            }
        }
        focusRequest = null
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        stopForeground(android.app.Service.STOP_FOREGROUND_REMOVE)
    }

    private fun startForegroundNow(id: String) {
        val ringIntent = Intent(this, AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmRingActivity.EXTRA_ALARM_ID, id)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            6001,
            ringIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setColor(0xFFF59E0B.toInt())
            .setContentTitle(getString(R.string.alarm_notification_title))
            .setContentText(getString(R.string.alarm_notification_text))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSound(null)
            .setDefaults(0)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stat_alarm, getString(R.string.ring_action), pendingIntent)
            .build()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun createAlarmChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.alarm_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = getString(R.string.alarm_channel_description)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            },
        )
    }

    override fun onDestroy() {
        scope.cancel()
        stopAll()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.lemomation.lucidlogger.action.START_ALARM"
        const val ACTION_STOP = "com.lemomation.lucidlogger.action.STOP_ALARM"
        const val ACTION_PAUSE = "com.lemomation.lucidlogger.action.PAUSE_ALARM"
        const val ACTION_RESUME = "com.lemomation.lucidlogger.action.RESUME_ALARM"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val CHANNEL_ID = "lucid_alarms"
        const val NOTIFICATION_ID = 6002

        fun start(context: Context, alarmId: String) {
            val intent = Intent(context, AlarmService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ALARM_ID, alarmId)
            }
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmService::class.java))
        }

        fun pause(context: Context) {
            context.startService(Intent(context, AlarmService::class.java).apply { action = ACTION_PAUSE })
        }

        fun resume(context: Context) {
            context.startService(Intent(context, AlarmService::class.java).apply { action = ACTION_RESUME })
        }
    }
}

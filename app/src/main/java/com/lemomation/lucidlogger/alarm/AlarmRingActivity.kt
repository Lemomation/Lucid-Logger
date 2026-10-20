package com.lemomation.lucidlogger.alarm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lemomation.lucidlogger.LucidLoggerApp
import com.lemomation.lucidlogger.data.model.AppSettings
import com.lemomation.lucidlogger.ui.alarm.AlarmRingScreen
import com.lemomation.lucidlogger.ui.theme.LucidLoggerTheme

class AlarmRingActivity : ComponentActivity() {
    private var activeAlarmId by mutableStateOf<String?>(null)
    private var closing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeAlarmId = intent.getStringExtra(EXTRA_ALARM_ID)
        if (activeAlarmId.isNullOrBlank()) {
            finish()
            return
        }
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.Transparent.value.toInt()
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.Black.value.toInt()

        val container = (application as LucidLoggerApp).container
        setContent {
            LucidLoggerTheme {
                val settings by container.settings.settings.collectAsStateWithLifecycle(
                    initialValue = AppSettings(),
                )
                val id = activeAlarmId
                val alarm = remember(id) { id?.let { container.alarms.get(it) } }
                if (alarm == null) {
                    LaunchedEffect(id) { finishAndRemoveTask() }
                } else {
                    key(alarm.id) {
                        AlarmRingScreen(
                            container = container,
                            alarm = alarm,
                            settings = settings,
                            onFinished = {
                                closing = true
                                finishAndRemoveTask()
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent?.getStringExtra(EXTRA_ALARM_ID)?.let { activeAlarmId = it }
    }

    override fun onPause() {
        super.onPause()
        if (!isFinishing && !closing) {
            runCatching { AlarmService.resume(this) }
        }
    }

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
    }
}

package com.lemomation.lucidlogger.ui.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lemomation.lucidlogger.AppContainer
import com.lemomation.lucidlogger.data.model.Alarm
import com.lemomation.lucidlogger.data.model.AppSettings
import com.lemomation.lucidlogger.data.model.BuiltinSounds
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

class AlarmViewModel(private val container: AppContainer) : ViewModel() {
    val alarms: StateFlow<List<Alarm>> = container.alarms.alarms
    val settings: StateFlow<AppSettings> = container.settings.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

    init {
        viewModelScope.launch { container.alarms.ensureDefault() }
    }

    fun nextTrigger(alarm: Alarm): ZonedDateTime? = container.scheduler.nextTrigger(alarm)

    fun nextAlarm(): Pair<Alarm, ZonedDateTime>? {
        return alarms.value
            .filter { it.enabled }
            .mapNotNull { alarm -> container.scheduler.nextTrigger(alarm)?.let { alarm to it } }
            .minByOrNull { it.second.toInstant() }
    }

    fun save(alarm: Alarm) {
        viewModelScope.launch {
            container.alarms.upsert(alarm)
            container.scheduler.schedule(alarm)
        }
    }

    fun setEnabled(alarm: Alarm, enabled: Boolean) {
        viewModelScope.launch {
            val next = alarm.copy(enabled = enabled)
            container.alarms.upsert(next)
            container.scheduler.schedule(next)
        }
    }

    fun toggleDay(alarm: Alarm, day: Int) {
        val days = if (day in alarm.days) alarm.days - day else alarm.days + day
        save(alarm.copy(days = days))
    }

    fun delete(alarm: Alarm) {
        viewModelScope.launch {
            container.scheduler.cancel(alarm)
            container.alarms.delete(alarm.id)
        }
    }

    fun setDefaultSound(soundId: String) {
        viewModelScope.launch {
            container.settings.update { it.copy(defaultSoundId = soundId) }
        }
    }

    fun setSound(alarm: Alarm, soundId: String, uri: String? = null, name: String? = null) {
        save(
            alarm.copy(
                soundId = soundId,
                customSoundUri = uri,
                customSoundName = name,
                enabled = alarm.enabled,
            ),
        )
    }

    fun newAlarm(): Alarm = Alarm.default().copy(
        id = "alarm-${System.currentTimeMillis()}",
        hour = 5,
        minute = 15,
        enabled = false,
        soundId = settings.value.defaultSoundId.ifBlank { BuiltinSounds.SINGING_BOWL },
        volumeRampSeconds = settings.value.volumeRampSeconds,
    )

    fun canScheduleExactAlarms(): Boolean = container.scheduler.canScheduleExactAlarms()
}

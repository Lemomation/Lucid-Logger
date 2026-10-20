package com.lemomation.lucidlogger.data.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class AppSettings(
    val minVoiceSeconds: Int = 30,
    val minNoteChars: Int = 120,
    val enforceOnAllSaves: Boolean = false,
    val reRingOnEarlyStop: Boolean = true,
    val emergencyDismissEnabled: Boolean = true,
    val emergencyDismissDelayMinutes: Int = 3,
    val snoozeEnabled: Boolean = false,
    val snoozeMinutes: Int = 10,
    val maxSnoozes: Int = 3,
    val defaultSoundId: String = BuiltinSounds.SINGING_BOWL,
    val volumeRampSeconds: Int = 30,
    val vibrationPattern: String = "gentle",
    val alarmVolume: Float = 1f,
    val morningEnrichment: Boolean = true,
    val morningReminder: Boolean = true,
    val morningReminderHour: Int = 8,
    val morningReminderMinute: Int = 0,
    val exportUri: String? = null,
    val autoExport: Boolean = false,
    val autoWikilinks: Boolean = true,
    val dimAtNight: Boolean = true,
    val dimLevel: Float = 0.15f,
    val keepScreenOn: Boolean = true,
    val pureBlack: Boolean = false,
    val reduceMotion: Boolean = false,
    val use24Hour: Boolean = false,
    val weekStartsMonday: Boolean = false,
    val textScale: Float = 1f,
) {
    fun today(): LocalDate = LocalDate.now()
    fun nowTime(): LocalTime = LocalTime.now(ZoneId.systemDefault())
}

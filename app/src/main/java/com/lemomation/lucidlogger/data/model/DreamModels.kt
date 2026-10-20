package com.lemomation.lucidlogger.data.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime

enum class DreamSource {
    VOICE,
    NOTE,
    MIXED,
}

data class DreamNote(
    val id: String,
    val fileName: String,
    val title: String,
    val body: String,
    val created: OffsetDateTime,
    val updated: OffsetDateTime?,
    val tags: List<String>,
    val links: List<String>,
    val audioFile: String?,
    val audioDuration: Int?,
    val waveform: List<Int>,
    val lucid: Boolean,
    val source: DreamSource,
) {
    val date: LocalDate get() = created.toLocalDate()
    val time: LocalTime get() = created.toLocalTime()
    val hasAudio: Boolean get() = !audioFile.isNullOrBlank()
}

@Serializable
data class Alarm(
    val id: String,
    val hour: Int,
    val minute: Int,
    val days: Set<Int>,
    val enabled: Boolean,
    val label: String,
    val soundId: String,
    val customSoundUri: String? = null,
    val customSoundName: String? = null,
    val vibrate: Boolean = true,
    val volumeRampSeconds: Int = 30,
) {
    val timeLabel: String
        get() = "%02d:%02d".format(hour, minute)

    fun daysLabel(): String {
        if (days.isEmpty()) return "One time"
        if (days.size == 7) return "Every day"
        if (days == setOf(1, 2, 3, 4, 5)) return "Weekdays"
        if (days == setOf(6, 7)) return "Weekends"
        return listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            .filterIndexed { index, _ -> (index + 1) in days }
            .joinToString(" ")
    }

    companion object {
        fun default(index: Int = 0): Alarm = Alarm(
            id = "alarm-${System.currentTimeMillis()}-$index",
            hour = 3,
            minute = 45,
            days = setOf(1, 2, 3, 4, 5, 6, 7),
            enabled = false,
            label = "Lucid dream alarm",
            soundId = BuiltinSounds.SINGING_BOWL,
            vibrate = true,
            volumeRampSeconds = 30,
        )
    }
}

object BuiltinSounds {
    const val SINGING_BOWL = "singing_bowl"
    const val SOFT_CHIMES = "soft_chimes"
    const val AMBIENT_DRONE = "ambient_drone"
    const val RAIN_TENT = "rain_tent"
    const val CUSTOM = "custom"
}

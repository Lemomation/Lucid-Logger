package com.lemomation.lucidlogger.audio

import android.content.Context
import com.lemomation.lucidlogger.R
import com.lemomation.lucidlogger.data.model.BuiltinSounds

data class BuiltinSound(
    val id: String,
    val name: String,
    val subtitle: String,
    val rawResId: Int,
)

object BuiltinSoundCatalog {
    fun all(context: Context): List<BuiltinSound> = listOf(
        BuiltinSound(BuiltinSounds.SINGING_BOWL, "Tibetan Singing Bowl", "Warm, slow, resonant", R.raw.alarm_singing_bowl),
        BuiltinSound(BuiltinSounds.SOFT_CHIMES, "Soft Chimes", "Gentle 90s rise", R.raw.alarm_soft_chimes),
        BuiltinSound(BuiltinSounds.AMBIENT_DRONE, "Ambient Drone", "Low, loopable pad", R.raw.alarm_ambient_drone),
        BuiltinSound(BuiltinSounds.RAIN_TENT, "Rain on Tent", "Soft white noise", R.raw.alarm_rain_tent),
    )

    fun resId(id: String): Int = when (id) {
        BuiltinSounds.SOFT_CHIMES -> R.raw.alarm_soft_chimes
        BuiltinSounds.AMBIENT_DRONE -> R.raw.alarm_ambient_drone
        BuiltinSounds.RAIN_TENT -> R.raw.alarm_rain_tent
        else -> R.raw.alarm_singing_bowl
    }

    fun name(id: String): String = when (id) {
        BuiltinSounds.SOFT_CHIMES -> "Soft Chimes"
        BuiltinSounds.AMBIENT_DRONE -> "Ambient Drone"
        BuiltinSounds.RAIN_TENT -> "Rain on Tent"
        BuiltinSounds.CUSTOM -> "Custom sound"
        else -> "Tibetan Singing Bowl"
    }
}

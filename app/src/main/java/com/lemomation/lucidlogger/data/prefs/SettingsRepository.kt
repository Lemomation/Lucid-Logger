package com.lemomation.lucidlogger.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lemomation.lucidlogger.data.model.AppSettings
import com.lemomation.lucidlogger.data.model.BuiltinSounds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "lucid_settings")

class SettingsRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            minVoiceSeconds = prefs[Keys.MIN_VOICE] ?: 30,
            minNoteChars = prefs[Keys.MIN_NOTE] ?: 120,
            enforceOnAllSaves = prefs[Keys.ENFORCE_ALL] ?: false,
            reRingOnEarlyStop = prefs[Keys.RERING] ?: true,
            emergencyDismissEnabled = prefs[Keys.EMERGENCY] ?: true,
            emergencyDismissDelayMinutes = prefs[Keys.EMERGENCY_DELAY] ?: 3,
            snoozeEnabled = prefs[Keys.SNOOZE] ?: false,
            snoozeMinutes = prefs[Keys.SNOOZE_MINUTES] ?: 10,
            maxSnoozes = prefs[Keys.MAX_SNOOZES] ?: 3,
            defaultSoundId = prefs[Keys.DEFAULT_SOUND] ?: BuiltinSounds.SINGING_BOWL,
            volumeRampSeconds = prefs[Keys.RAMP] ?: 30,
            vibrationPattern = prefs[Keys.VIBRATION] ?: "gentle",
            alarmVolume = prefs[Keys.ALARM_VOLUME] ?: 1f,
            morningEnrichment = prefs[Keys.MORNING_ENRICH] ?: true,
            morningReminder = prefs[Keys.MORNING_REMINDER] ?: true,
            morningReminderHour = prefs[Keys.MORNING_HOUR] ?: 8,
            morningReminderMinute = prefs[Keys.MORNING_MINUTE] ?: 0,
            exportUri = prefs[Keys.EXPORT_URI]?.takeIf { it.isNotBlank() },
            autoExport = prefs[Keys.AUTO_EXPORT] ?: false,
            autoWikilinks = prefs[Keys.AUTO_LINKS] ?: true,
            dimAtNight = prefs[Keys.DIM] ?: true,
            dimLevel = prefs[Keys.DIM_LEVEL] ?: 0.15f,
            keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: true,
            pureBlack = prefs[Keys.PURE_BLACK] ?: false,
            reduceMotion = prefs[Keys.REDUCE_MOTION] ?: false,
            use24Hour = prefs[Keys.USE_24H] ?: false,
            weekStartsMonday = prefs[Keys.WEEK_MONDAY] ?: false,
            textScale = prefs[Keys.TEXT_SCALE] ?: 1f,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        val current = current()
        val next = transform(current)
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.MIN_VOICE] = next.minVoiceSeconds.coerceIn(5, 180)
            prefs[Keys.MIN_NOTE] = next.minNoteChars.coerceIn(20, 1000)
            prefs[Keys.ENFORCE_ALL] = next.enforceOnAllSaves
            prefs[Keys.RERING] = next.reRingOnEarlyStop
            prefs[Keys.EMERGENCY] = next.emergencyDismissEnabled
            prefs[Keys.EMERGENCY_DELAY] = next.emergencyDismissDelayMinutes.coerceIn(1, 15)
            prefs[Keys.SNOOZE] = next.snoozeEnabled
            prefs[Keys.SNOOZE_MINUTES] = next.snoozeMinutes.coerceIn(1, 60)
            prefs[Keys.MAX_SNOOZES] = next.maxSnoozes.coerceIn(0, 10)
            prefs[Keys.DEFAULT_SOUND] = next.defaultSoundId
            prefs[Keys.RAMP] = next.volumeRampSeconds.coerceIn(0, 180)
            prefs[Keys.VIBRATION] = next.vibrationPattern
            prefs[Keys.ALARM_VOLUME] = next.alarmVolume.coerceIn(0.1f, 1f)
            prefs[Keys.MORNING_ENRICH] = next.morningEnrichment
            prefs[Keys.MORNING_REMINDER] = next.morningReminder
            prefs[Keys.MORNING_HOUR] = next.morningReminderHour.coerceIn(0, 23)
            prefs[Keys.MORNING_MINUTE] = next.morningReminderMinute.coerceIn(0, 59)
            prefs[Keys.EXPORT_URI] = next.exportUri.orEmpty()
            prefs[Keys.AUTO_EXPORT] = next.autoExport
            prefs[Keys.AUTO_LINKS] = next.autoWikilinks
            prefs[Keys.DIM] = next.dimAtNight
            prefs[Keys.DIM_LEVEL] = next.dimLevel.coerceIn(0.05f, 0.6f)
            prefs[Keys.KEEP_SCREEN_ON] = next.keepScreenOn
            prefs[Keys.PURE_BLACK] = next.pureBlack
            prefs[Keys.REDUCE_MOTION] = next.reduceMotion
            prefs[Keys.USE_24H] = next.use24Hour
            prefs[Keys.WEEK_MONDAY] = next.weekStartsMonday
            prefs[Keys.TEXT_SCALE] = next.textScale.coerceIn(0.85f, 1.3f)
        }
    }

    suspend fun setExportUri(uri: String?) {
        update { it.copy(exportUri = uri) }
    }

    private object Keys {
        val MIN_VOICE = intPreferencesKey("min_voice_seconds")
        val MIN_NOTE = intPreferencesKey("min_note_chars")
        val ENFORCE_ALL = booleanPreferencesKey("enforce_all_saves")
        val RERING = booleanPreferencesKey("re_ring_early_stop")
        val EMERGENCY = booleanPreferencesKey("emergency_dismiss")
        val EMERGENCY_DELAY = intPreferencesKey("emergency_delay_minutes")
        val SNOOZE = booleanPreferencesKey("snooze_enabled")
        val SNOOZE_MINUTES = intPreferencesKey("snooze_minutes")
        val MAX_SNOOZES = intPreferencesKey("max_snoozes")
        val DEFAULT_SOUND = stringPreferencesKey("default_sound")
        val RAMP = intPreferencesKey("volume_ramp_seconds")
        val VIBRATION = stringPreferencesKey("vibration_pattern")
        val ALARM_VOLUME = floatPreferencesKey("alarm_volume")
        val MORNING_ENRICH = booleanPreferencesKey("morning_enrichment")
        val MORNING_REMINDER = booleanPreferencesKey("morning_reminder")
        val MORNING_HOUR = intPreferencesKey("morning_hour")
        val MORNING_MINUTE = intPreferencesKey("morning_minute")
        val EXPORT_URI = stringPreferencesKey("export_uri")
        val AUTO_EXPORT = booleanPreferencesKey("auto_export")
        val AUTO_LINKS = booleanPreferencesKey("auto_wikilinks")
        val DIM = booleanPreferencesKey("dim_at_night")
        val DIM_LEVEL = floatPreferencesKey("dim_level")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val PURE_BLACK = booleanPreferencesKey("pure_black")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val USE_24H = booleanPreferencesKey("use_24_hour")
        val WEEK_MONDAY = booleanPreferencesKey("week_starts_monday")
        val TEXT_SCALE = floatPreferencesKey("text_scale")
    }
}

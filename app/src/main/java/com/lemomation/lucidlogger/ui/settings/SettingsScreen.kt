package com.lemomation.lucidlogger.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.lemomation.lucidlogger.AppContainer
import com.lemomation.lucidlogger.BuildConfig
import com.lemomation.lucidlogger.alarm.MorningReminderScheduler
import com.lemomation.lucidlogger.audio.BuiltinSoundCatalog
import com.lemomation.lucidlogger.data.model.AppSettings
import com.lemomation.lucidlogger.data.model.BuiltinSounds
import com.lemomation.lucidlogger.ui.components.LucidCard
import com.lemomation.lucidlogger.ui.components.LucidDivider
import com.lemomation.lucidlogger.ui.components.LucidSwitch
import com.lemomation.lucidlogger.ui.components.LucidTimePickerDialog
import com.lemomation.lucidlogger.ui.components.LucidTopBar
import com.lemomation.lucidlogger.ui.components.PrimaryButton
import com.lemomation.lucidlogger.ui.components.SectionHeader
import com.lemomation.lucidlogger.ui.components.SettingRow
import com.lemomation.lucidlogger.ui.components.ValueStepper
import com.lemomation.lucidlogger.ui.theme.LucidAmber
import com.lemomation.lucidlogger.ui.theme.LucidBorder
import com.lemomation.lucidlogger.ui.theme.LucidCanvas
import com.lemomation.lucidlogger.ui.theme.LucidError
import com.lemomation.lucidlogger.ui.theme.LucidPrimary
import com.lemomation.lucidlogger.ui.theme.LucidSurface2
import com.lemomation.lucidlogger.ui.theme.LucidSurface3
import com.lemomation.lucidlogger.ui.theme.LucidText
import com.lemomation.lucidlogger.ui.theme.LucidTextSubtle
import com.lemomation.lucidlogger.ui.theme.LucidTextVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    container: AppContainer,
    settings: AppSettings,
    notesCount: Int,
    onOpenAlarm: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showMorningTime by remember { mutableStateOf(false) }
    var showDeleteAll by remember { mutableStateOf(false) }
    var storageBytes by remember { mutableStateOf(0L) }

    LaunchedEffect(notesCount) {
        storageBytes = withContext(Dispatchers.IO) { container.dreams.storageBytes() }
    }

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun update(transform: (AppSettings) -> AppSettings, morning: Boolean = false) {
        scope.launch {
            container.settings.update(transform)
            if (morning) {
                MorningReminderScheduler(context).schedule(container.settings.current())
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            scope.launch {
                container.settings.setExportUri(uri.toString())
                toast("Obsidian export folder connected")
            }
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            scope.launch {
                container.exportManager.writeBackup(uri)
                    .onSuccess { toast("Vault backup written") }
                    .onFailure { toast("Backup failed: ${it.message ?: "unknown error"}") }
            }
        }
    }

    val exportFolderName = remember(settings.exportUri) {
        settings.exportUri?.let { raw ->
            runCatching { DocumentFile.fromTreeUri(context, Uri.parse(raw))?.name }.getOrNull()
        }
    }

    Column(Modifier.fillMaxSize().background(LucidCanvas)) {
        LucidTopBar(title = "Settings")
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                LucidCard(
                    padding = PaddingValues(15.dp),
                    onClick = onOpenAlarm,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(LucidAmber.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.Alarm, contentDescription = null, tint = LucidAmber)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Alarm readiness", style = MaterialTheme.typography.titleMedium, color = LucidText)
                            Text(
                                "Sounds, full-screen wake, permissions and the emergency exit",
                                style = MaterialTheme.typography.bodySmall,
                                color = LucidTextSubtle,
                            )
                        }
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = LucidTextSubtle)
                    }
                }
            }

            item {
                SettingsSection("The wake gate", Icons.Outlined.Shield) {
                    SettingRow("Minimum voice", "Recorded seconds before the alarm releases") {
                        ValueStepper(
                            value = settings.minVoiceSeconds,
                            onDecrease = { update { s -> s.copy(minVoiceSeconds = (s.minVoiceSeconds - 5).coerceAtLeast(5)) } },
                            onIncrease = { update { s -> s.copy(minVoiceSeconds = (s.minVoiceSeconds + 5).coerceAtMost(180)) } },
                            suffix = "s",
                        )
                    }
                    LucidDivider()
                    SettingRow("Minimum written note", "Characters of real dream text") {
                        ValueStepper(
                            value = settings.minNoteChars,
                            onDecrease = { update { s -> s.copy(minNoteChars = (s.minNoteChars - 20).coerceAtLeast(20)) } },
                            onIncrease = { update { s -> s.copy(minNoteChars = (s.minNoteChars + 20).coerceAtMost(1000)) } },
                            suffix = "",
                        )
                    }
                    LucidDivider()
                    SettingRow("Enforce while journaling", "Apply the same threshold to normal saves") {
                        LucidSwitch(settings.enforceOnAllSaves) { checked ->
                            update { it.copy(enforceOnAllSaves = checked) }
                        }
                    }
                    LucidDivider()
                    SettingRow("Re-ring on early stop", "The alarm returns if you leave before the gate") {
                        LucidSwitch(settings.reRingOnEarlyStop) { checked ->
                            update { it.copy(reRingOnEarlyStop = checked) }
                        }
                    }
                    LucidDivider()
                    SettingRow("Emergency exit", "Hold a hidden button to dismiss without recording") {
                        LucidSwitch(settings.emergencyDismissEnabled) { checked ->
                            update { it.copy(emergencyDismissEnabled = checked) }
                        }
                    }
                    if (settings.emergencyDismissEnabled) {
                        SettingRow("Reveal after", "Minutes of ringing before the exit appears") {
                            ValueStepper(
                                value = settings.emergencyDismissDelayMinutes,
                                onDecrease = { update { s -> s.copy(emergencyDismissDelayMinutes = (s.emergencyDismissDelayMinutes - 1).coerceAtLeast(1)) } },
                                onIncrease = { update { s -> s.copy(emergencyDismissDelayMinutes = (s.emergencyDismissDelayMinutes + 1).coerceAtMost(15)) } },
                                suffix = "m",
                            )
                        }
                    }
                }
            }

            item {
                SettingsSection("Snooze", Icons.Outlined.NotificationsActive) {
                    SettingRow("Allow snooze", "Off by default. The alarm still returns.") {
                        LucidSwitch(settings.snoozeEnabled) { checked ->
                            update { it.copy(snoozeEnabled = checked) }
                        }
                    }
                    if (settings.snoozeEnabled) {
                        LucidDivider()
                        SettingRow("Snooze length", "Quiet minutes before the alarm returns") {
                            ValueStepper(
                                value = settings.snoozeMinutes,
                                onDecrease = { update { s -> s.copy(snoozeMinutes = (s.snoozeMinutes - 1).coerceAtLeast(1)) } },
                                onIncrease = { update { s -> s.copy(snoozeMinutes = (s.snoozeMinutes + 1).coerceAtMost(60)) } },
                                suffix = "m",
                            )
                        }
                        LucidDivider()
                        SettingRow("Maximum snoozes", "Then the gate must be met") {
                            ValueStepper(
                                value = settings.maxSnoozes,
                                onDecrease = { update { s -> s.copy(maxSnoozes = (s.maxSnoozes - 1).coerceAtLeast(0)) } },
                                onIncrease = { update { s -> s.copy(maxSnoozes = (s.maxSnoozes + 1).coerceAtMost(10)) } },
                                suffix = "",
                            )
                        }
                    }
                }
            }

            item {
                SettingsSection("Morning", Icons.Outlined.Bedtime) {
                    SettingRow("Morning enrichment", "Keep a place to finish voice-only dreams") {
                        LucidSwitch(settings.morningEnrichment) { checked ->
                            update { it.copy(morningEnrichment = checked) }
                        }
                    }
                    LucidDivider()
                    SettingRow("Morning reminder", "One quiet nudge if a memo has no details") {
                        LucidSwitch(settings.morningReminder) { checked ->
                            update({ it.copy(morningReminder = checked) }, morning = true)
                        }
                    }
                    if (settings.morningReminder) {
                        LucidDivider()
                        SettingRow("Reminder time", "Local time, every day") {
                            Text(
                                formatTime(settings.morningReminderHour, settings.morningReminderMinute, settings.use24Hour),
                                style = MaterialTheme.typography.labelLarge,
                                color = LucidAmber,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(LucidSurface3)
                                    .clickable { showMorningTime = true }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }

            item {
                SettingsSection("Obsidian vault", Icons.Outlined.IosShare) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (settings.exportUri != null) LucidPrimary.copy(alpha = 0.14f) else LucidSurface3),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (settings.exportUri != null) Icons.Filled.Folder else Icons.Outlined.CloudOff,
                                contentDescription = null,
                                tint = if (settings.exportUri != null) LucidPrimary else LucidTextSubtle,
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                exportFolderName ?: "No folder connected",
                                style = MaterialTheme.typography.bodyMedium,
                                color = LucidText,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                if (settings.exportUri != null) "Markdown and audio export here"
                                else "Choose where Lucid Logger writes .md files",
                                style = MaterialTheme.typography.bodySmall,
                                color = LucidTextSubtle,
                            )
                        }
                        TextButton(onClick = { exportLauncher.launch(null) }) {
                            Text(if (settings.exportUri == null) "Choose" else "Change", color = LucidAmber)
                        }
                    }
                    LucidDivider()
                    SettingRow("Auto-export new dreams", "Write the note and audio as soon as you save") {
                        LucidSwitch(
                            checked = settings.autoExport,
                            enabled = settings.exportUri != null,
                        ) { checked ->
                            update { it.copy(autoExport = checked) }
                        }
                    }
                    LucidDivider()
                    Text(
                        "Lucid Logger keeps its own private copy. Export is one-way; edits made in Obsidian are not imported.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LucidTextSubtle,
                        lineHeight = 17.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        PrimaryButton(
                            text = "Export all",
                            onClick = {
                                val raw = settings.exportUri
                                if (raw == null) {
                                    exportLauncher.launch(null)
                                } else {
                                    scope.launch {
                                        container.exportManager.exportAll(Uri.parse(raw))
                                            .onSuccess { toast("Exported $it dreams") }
                                            .onFailure { toast("Export failed: ${it.message ?: "unknown error"}") }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.FileDownload,
                            enabled = notesCount > 0,
                        )
                        PrimaryButton(
                            text = "Backup",
                            onClick = { backupLauncher.launch("lucid-logger-backup.zip") },
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Description,
                            accent = false,
                        )
                    }
                }
            }

            item {
                SettingsSection("Alarm sound", Icons.Outlined.VolumeUp) {
                    SettingRow(
                        "Default sound",
                        "Used by new alarms. Change per alarm on the Alarm screen.",
                    ) {
                        Text(
                            soundName(settings.defaultSoundId),
                            style = MaterialTheme.typography.labelMedium,
                            color = LucidAmber,
                        )
                    }
                    LucidDivider()
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        soundOptions(settings.defaultSoundId).forEach { id ->
                            SoundPill(
                                name = opportunityName(id),
                                selected = settings.defaultSoundId == id,
                                onClick = { update { it.copy(defaultSoundId = id) } },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            item {
                SettingsSection("Appearance & rhythm", Icons.Outlined.Palette) {
                    SettingRow("24-hour time", "Alarms, journal timestamps and calendar") {
                        LucidSwitch(settings.use24Hour) { checked -> update { it.copy(use24Hour = checked) } }
                    }
                    LucidDivider()
                    SettingRow("Week starts Monday", "Calendar heatmap alignment") {
                        LucidSwitch(settings.weekStartsMonday) { checked -> update { it.copy(weekStartsMonday = checked) } }
                    }
                    LucidDivider()
                    SettingRow("Keep screen awake", "While Lucid Logger is open") {
                        LucidSwitch(settings.keepScreenOn) { checked -> update { it.copy(keepScreenOn = checked) } }
                    }
                    LucidDivider()
                    SettingRow("Text size", "Applies across the whole vault") {
                        ValueStepper(
                            value = (settings.textScale * 100).toInt(),
                            onDecrease = { update { s -> s.copy(textScale = ((s.textScale - 0.05f).coerceAtLeast(0.85f))) } },
                            onIncrease = { update { s -> s.copy(textScale = ((s.textScale + 0.05f).coerceAtMost(1.30f))) } },
                            suffix = "%",
                        )
                    }
                    LucidDivider()
                    SettingRow("Auto-wikilink known motifs", "Link recurring names and places as you write") {
                        LucidSwitch(settings.autoWikilinks) { checked -> update { it.copy(autoWikilinks = checked) } }
                    }
                }
            }

            item {
                SettingsSection("Vault & privacy", Icons.Outlined.Lock) {
                    SettingRow("App-private storage", "No account, no cloud, no network sync") {
                        Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = LucidPrimary, modifier = Modifier.size(19.dp))
                    }
                    LucidDivider()
                    SettingRow("Dreams captured", "Markdown notes in this vault") {
                        Text(notesCount.toString(), style = MaterialTheme.typography.labelLarge, color = LucidText)
                    }
                    LucidDivider()
                    SettingRow("Storage used", "Notes, audio and metadata") {
                        Text(formatBytes(storageBytes), style = MaterialTheme.typography.labelLarge, color = LucidText)
                    }
                    LucidDivider()
                    Text(
                        "Deleting a dream removes its note and audio immediately. Export a backup first if you need one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LucidTextSubtle,
                        lineHeight = 17.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { showDeleteAll = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.DeleteForever, contentDescription = null, tint = LucidError, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Delete all dreams", color = LucidError)
                    }
                }
            }

            item {
                SectionHeader("About", Icons.Outlined.AutoStories)
            }
            item {
                LucidCard {
                    Text("Lucid Logger ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium, color = LucidText)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "A quiet vault for the dreams you bring back. Notes stay on this device unless you export them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LucidTextVariant,
                        lineHeight = 18.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Fact("Markdown")
                        Fact("AAC audio")
                        Fact("Offline")
                    }
                }
            }
        }
    }

    if (showMorningTime) {
        LucidTimePickerDialog(
            initialHour = settings.morningReminderHour,
            initialMinute = settings.morningReminderMinute,
            use24Hour = settings.use24Hour,
            onDismiss = { showMorningTime = false },
            onConfirm = { hour, minute ->
                showMorningTime = false
                update({
                    it.copy(morningReminderHour = hour, morningReminderMinute = minute)
                }, morning = true)
            },
        )
    }

    if (showDeleteAll) {
        AlertDialog(
            onDismissRequest = { showDeleteAll = false },
            containerColor = LucidSurface2,
            title = { Text("Delete every dream?", color = LucidText) },
            text = { Text("This cannot be undone. Export a backup first if these nights matter.", color = LucidTextVariant) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteAll = false
                    scope.launch {
                        container.dreams.deleteAll()
                        toast("All dreams deleted")
                    }
                }) { Text("Delete all", color = LucidError) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAll = false }) { Text("Cancel", color = LucidTextVariant) }
            },
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title, icon)
        LucidCard(content = content)
    }
}

@Composable
private fun SoundPill(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(CircleShape)
            .background(if (selected) LucidAmber.copy(alpha = 0.13f) else LucidSurface3)
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            name,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) LucidAmber else LucidTextVariant,
            maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun Fact(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = LucidTextSubtle,
        modifier = Modifier
            .clip(CircleShape)
            .background(LucidSurface3)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

private fun soundOptions(selected: String): List<String> {
    val all = listOf(
        BuiltinSounds.SINGING_BOWL,
        BuiltinSounds.SOFT_CHIMES,
        BuiltinSounds.AMBIENT_DRONE,
        BuiltinSounds.RAIN_TENT,
    )
    return if (selected in all) all else all
}

private fun soundName(id: String): String = BuiltinSoundCatalog.name(id)

private fun opportunityName(id: String): String = when (id) {
    BuiltinSounds.SINGING_BOWL -> "Bowl"
    BuiltinSounds.SOFT_CHIMES -> "Chimes"
    BuiltinSounds.AMBIENT_DRONE -> "Drone"
    BuiltinSounds.RAIN_TENT -> "Rain"
    else -> "Sound"
}

private fun formatTime(hour: Int, minute: Int, use24Hour: Boolean): String =
    if (use24Hour) "%02d:%02d".format(hour, minute)
    else {
        val suffix = if (hour < 12) "AM" else "PM"
        val display = when {
            hour % 12 == 0 -> 12
            else -> hour % 12
        }
        "%d:%02d %s".format(display, minute, suffix)
    }

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.0f KB".format(bytes / 1024f)
    else -> "%.1f MB".format(bytes / (1024f * 1024f))
}

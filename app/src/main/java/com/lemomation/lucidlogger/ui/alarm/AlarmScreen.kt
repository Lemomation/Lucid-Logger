package com.lemomation.lucidlogger.ui.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lemomation.lucidlogger.LucidLoggerApp
import com.lemomation.lucidlogger.audio.BuiltinSoundCatalog
import com.lemomation.lucidlogger.audio.SoundPreview
import com.lemomation.lucidlogger.data.model.Alarm
import com.lemomation.lucidlogger.data.model.AppSettings
import com.lemomation.lucidlogger.data.model.BuiltinSounds
import com.lemomation.lucidlogger.ui.SimpleViewModelFactory
import com.lemomation.lucidlogger.ui.components.GhostButton
import com.lemomation.lucidlogger.ui.components.LucidCard
import com.lemomation.lucidlogger.ui.components.LucidDivider
import com.lemomation.lucidlogger.ui.components.LucidInput
import com.lemomation.lucidlogger.ui.components.LucidSwitch
import com.lemomation.lucidlogger.ui.components.LucidTimePickerDialog
import com.lemomation.lucidlogger.ui.components.LucidTopBar
import com.lemomation.lucidlogger.ui.components.PrimaryButton
import com.lemomation.lucidlogger.ui.components.SectionHeader
import com.lemomation.lucidlogger.ui.components.SettingRow
import com.lemomation.lucidlogger.ui.components.StatusPill
import com.lemomation.lucidlogger.ui.components.ValueStepper
import com.lemomation.lucidlogger.ui.theme.LucidAmber
import com.lemomation.lucidlogger.ui.theme.LucidBorder
import com.lemomation.lucidlogger.ui.theme.LucidBorderStrong
import com.lemomation.lucidlogger.ui.theme.LucidCanvas
import com.lemomation.lucidlogger.ui.theme.LucidError
import com.lemomation.lucidlogger.ui.theme.LucidOnAmber
import com.lemomation.lucidlogger.ui.theme.LucidPrimary
import com.lemomation.lucidlogger.ui.theme.LucidPrimarySoft
import com.lemomation.lucidlogger.ui.theme.LucidSurface
import com.lemomation.lucidlogger.ui.theme.LucidSurface2
import com.lemomation.lucidlogger.ui.theme.LucidSurface3
import com.lemomation.lucidlogger.ui.theme.LucidText
import com.lemomation.lucidlogger.ui.theme.LucidTextSubtle
import com.lemomation.lucidlogger.ui.theme.LucidTextVariant
import com.lemomation.lucidlogger.util.TextUtils
import kotlinx.coroutines.delay
import java.time.ZonedDateTime

@Composable
fun AlarmScreen(
    onOpenSettings: () -> Unit,
    onQuickRecord: () -> Unit,
) {
    val context = LocalContext.current
    val container = (context.applicationContext as LucidLoggerApp).container
    val vm: AlarmViewModel = viewModel(factory = SimpleViewModelFactory { AlarmViewModel(container) })
    val alarms by vm.alarms.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    var nowTick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            nowTick = System.currentTimeMillis()
        }
    }

    val primary = alarms.firstOrNull()
    val nextPair = remember(alarms, nowTick) { vm.nextAlarm() }
    val dialAlarm = nextPair?.first ?: primary
    val nextTrigger = nextPair?.second ?: dialAlarm?.let { vm.nextTrigger(it) }

    val preview = remember { SoundPreview(context) }
    val playingId by preview.playingId.collectAsStateWithLifecycle()
    DisposableEffect(Unit) { onDispose { preview.stop() } }

    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    var customSoundTarget by remember { mutableStateOf<Alarm?>(null) }
    var permissionRefresh by remember { mutableIntStateOf(0) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { permissionRefresh++ }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { permissionRefresh++ }
    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { permissionRefresh++ }
    val customSoundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            customSoundTarget?.let { target ->
                vm.setSound(
                    target,
                    BuiltinSounds.CUSTOM,
                    uri.toString(),
                    displayName(context, uri) ?: "Custom alarm sound",
                )
                permissionRefresh++
            }
        }
        customSoundTarget = null
    }

    val sounds = remember(context) { BuiltinSoundCatalog.all(context) }

    Column(Modifier.fillMaxSize().background(LucidCanvas)) {
        LucidTopBar(
            actions = {
                IconButton(onClick = onQuickRecord) {
                    Icon(Icons.Filled.Mic, contentDescription = "Record a dream", tint = LucidPrimary)
                }
            },
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                AlarmDial(
                    alarm = dialAlarm,
                    nextTrigger = nextTrigger,
                    use24Hour = settings.use24Hour,
                    onClick = { showTimePicker = true },
                )
            }

            item {
                LucidCard(padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(LucidSurface2)
                                .border(1.dp, LucidBorder, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Outlined.Alarm,
                                contentDescription = null,
                                tint = LucidAmber,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (dialAlarm?.enabled == true) "Active tonight" else "Alarm paused",
                                style = MaterialTheme.typography.titleMedium,
                                color = LucidText,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                dialAlarm?.let { "Alarm set for ${formatTime(it.hour, it.minute, settings.use24Hour)}" }
                                    ?: "No alarm yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = LucidTextVariant,
                            )
                        }
                        LucidSwitch(
                            checked = dialAlarm?.enabled == true,
                            onCheckedChange = { enabled ->
                                val alarm = dialAlarm ?: vm.newAlarm()
                                vm.setEnabled(alarm, enabled)
                                if (enabled && Build.VERSION.SDK_INT >= 33 &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS,
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                        )
                    }
                }
            }

            item {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "REPEAT DAYS",
                            style = MaterialTheme.typography.labelSmall,
                            color = LucidTextSubtle,
                            letterSpacing = 1.sp,
                        )
                        Text(
                            dialAlarm?.daysLabel() ?: "—",
                            style = MaterialTheme.typography.labelSmall,
                            color = LucidPrimarySoft,
                        )
                    }
                    Spacer(Modifier.height(9.dp))
                    AlarmDaySelector(
                        selected = dialAlarm?.days.orEmpty(),
                        onToggle = { day ->
                            dialAlarm?.let { vm.toggleDay(it, day) }
                        },
                    )
                }
            }

            item {
                AntiLazyCard(
                    settings = settings,
                    onClick = onOpenSettings,
                )
            }

            item {
                SoundSection(
                    alarm = dialAlarm,
                    sounds = sounds,
                    playingId = playingId,
                    onPreview = { sound -> preview.toggle(sound.id, sound.rawResId) },
                    onSelect = { sound ->
                        dialAlarm?.let { vm.setSound(it, sound.id) }
                        vm.setDefaultSound(sound.id)
                    },
                    onUpload = {
                        customSoundTarget = dialAlarm
                        if (customSoundTarget != null) {
                            customSoundLauncher.launch(arrayOf("audio/*"))
                        }
                    },
                )
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionHeader("All alarms", Icons.Outlined.Alarm)
                    TextButton(onClick = { editingAlarm = vm.newAlarm() }) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = LucidPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add alarm", color = LucidPrimary, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (alarms.isEmpty()) {
                item {
                    LucidCard { Text("Preparing your first alarm…", color = LucidTextVariant) }
                }
            } else {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmListItem(
                        alarm = alarm,
                        settings = settings,
                        onClick = { editingAlarm = alarm },
                        onToggle = { enabled -> vm.setEnabled(alarm, enabled) },
                    )
                }
            }

            item {
                AlarmPermissionsCard(
                    context = context,
                    refreshKey = permissionRefresh,
                    onNotifications = {
                        if (Build.VERSION.SDK_INT >= 33) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            openNotificationSettings(context)
                        }
                    },
                    onExactAlarm = { openExactAlarmSettings(context) },
                    onFullScreen = { openFullScreenSettings(context) },
                    onMic = { micPermission.launch(Manifest.permission.RECORD_AUDIO) },
                    onBattery = { openBatterySettings(context) },
                )
            }
        }
    }

    if (showTimePicker && dialAlarm != null) {
        LucidTimePickerDialog(
            initialHour = dialAlarm.hour,
            initialMinute = dialAlarm.minute,
            use24Hour = settings.use24Hour,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                vm.save(dialAlarm.copy(hour = hour, minute = minute))
                showTimePicker = false
            },
        )
    }

    editingAlarm?.let { alarm ->
        AlarmEditorSheet(
            alarm = alarm,
            settings = settings,
            sounds = sounds,
            onDismiss = { editingAlarm = null },
            onSave = { updated ->
                vm.save(updated)
                editingAlarm = null
            },
            onDelete = {
                vm.delete(alarm)
                editingAlarm = null
            },
        )
    }
}

@Composable
private fun AlarmDial(
    alarm: Alarm?,
    nextTrigger: ZonedDateTime?,
    use24Hour: Boolean,
    onClick: () -> Unit,
) {
    val sweep by animateFloatAsState(
        targetValue = if (alarm?.enabled == true) 0.72f else 0f,
        label = "dialSweep",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(272.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(18.dp)) {
            val radius = size.minDimension / 2f - 8.dp.toPx()
            drawCircle(
                color = LucidSurface3,
                radius = radius,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 8.dp.toPx())),
                ),
            )
            val start = -90f
            val fullSweep = 360f
            drawArc(
                color = LucidAmber,
                startAngle = start,
                sweepAngle = fullSweep * sweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round),
            )
            drawCircle(
                color = LucidAmber.copy(alpha = 0.06f),
                radius = radius * 0.72f,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (alarm == null) {
                Text("No alarm", style = MaterialTheme.typography.headlineMedium, color = LucidTextVariant)
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (use24Hour) "%02d:%02d".format(alarm.hour, alarm.minute)
                        else "%02d:%02d".format(if (alarm.hour % 12 == 0) 12 else alarm.hour % 12, alarm.minute),
                        style = MaterialTheme.typography.displayMedium,
                        color = LucidText,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.SansSerif,
                    )
                    if (!use24Hour) {
                        Text(
                            text = if (alarm.hour < 12) "AM" else "PM",
                            style = MaterialTheme.typography.labelMedium,
                            color = LucidTextVariant,
                            modifier = Modifier.padding(start = 6.dp, bottom = 10.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = when {
                    alarm == null -> "Tap to set a wake time"
                    !alarm.enabled -> "Alarm paused for tonight"
                    nextTrigger != null -> TextUtils.formatRemaining(
                        nextTrigger.toInstant().toEpochMilli() - System.currentTimeMillis(),
                    )
                    else -> "Choose repeat days"
                },
                style = MaterialTheme.typography.bodySmall,
                color = LucidTextVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tap the dial to adjust",
                style = MaterialTheme.typography.labelSmall,
                color = LucidTextSubtle,
            )
        }
    }
}

@Composable
private fun AntiLazyCard(settings: AppSettings, onClick: () -> Unit) {
    LucidCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.LockOpen, contentDescription = null, tint = LucidAmber, modifier = Modifier.size(17.dp))
            Text(
                "TO TURN OFF THE ALARM",
                style = MaterialTheme.typography.labelSmall,
                color = LucidAmber,
                letterSpacing = 0.9.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "The alarm keeps ringing until you record at least ${settings.minVoiceSeconds} seconds of voice or write ${settings.minNoteChars} characters.",
            style = MaterialTheme.typography.bodyMedium,
            color = LucidText,
            lineHeight = 21.sp,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniThreshold(icon = Icons.Outlined.Mic, text = "${settings.minVoiceSeconds}s Whisper Recall")
            MiniThreshold(icon = Icons.Outlined.EditNote, text = "${settings.minNoteChars} Char Journal")
        }
    }
}

@Composable
private fun MiniThreshold(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(LucidSurface2)
            .border(1.dp, LucidBorder, RoundedCornerShape(9.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(icon, contentDescription = null, tint = LucidPrimarySoft, modifier = Modifier.size(15.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = LucidTextVariant, maxLines = 1)
    }
}

@Composable
private fun SoundSection(
    alarm: Alarm?,
    sounds: List<com.lemomation.lucidlogger.audio.BuiltinSound>,
    playingId: String?,
    onPreview: (com.lemomation.lucidlogger.audio.BuiltinSound) -> Unit,
    onSelect: (com.lemomation.lucidlogger.audio.BuiltinSound) -> Unit,
    onUpload: () -> Unit,
) {
    LucidCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.VolumeUp, contentDescription = null, tint = LucidAmber, modifier = Modifier.size(16.dp))
            Text("Sound", style = MaterialTheme.typography.titleMedium, color = LucidText, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            sounds.forEach { sound ->
                val selected = alarm?.soundId == sound.id
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) LucidSurface3 else LucidCanvas.copy(alpha = 0.4f))
                        .border(
                            1.dp,
                            if (selected) LucidAmber.copy(alpha = 0.55f) else LucidBorder,
                            RoundedCornerShape(10.dp),
                        )
                        .clickable { onSelect(sound) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (selected) LucidAmber else LucidSurface3)
                            .clickable { onPreview(sound) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (playingId == sound.id) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = "Preview",
                            tint = if (selected) LucidOnAmber else LucidTextVariant,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            sound.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LucidText,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        Text(sound.subtitle, style = MaterialTheme.typography.bodySmall, color = LucidTextSubtle)
                    }
                    if (selected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = LucidAmber, modifier = Modifier.size(18.dp))
                    }
                }
            }
            if (alarm?.soundId == BuiltinSounds.CUSTOM) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(LucidSurface3)
                        .border(1.dp, LucidAmber.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = LucidAmber, modifier = Modifier.size(17.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            alarm.customSoundName ?: "Custom alarm sound",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LucidText,
                        )
                        Text("Custom audio", style = MaterialTheme.typography.bodySmall, color = LucidTextSubtle)
                    }
                }
            }
            TextButton(onClick = onUpload, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.UploadFile, contentDescription = null, tint = LucidTextVariant, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("Upload custom audio (.mp3, .m4a, .wav)", color = LucidTextVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun AlarmListItem(
    alarm: Alarm,
    settings: AppSettings,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    LucidCard(onClick = onClick, padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        if (settings.use24Hour) "%02d:%02d".format(alarm.hour, alarm.minute)
                        else "%02d:%02d".format(if (alarm.hour % 12 == 0) 12 else alarm.hour % 12, alarm.minute),
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (alarm.enabled) LucidText else LucidTextSubtle,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Light,
                    )
                    if (!settings.use24Hour) {
                        Text(
                            if (alarm.hour < 12) " AM" else " PM",
                            style = MaterialTheme.typography.labelSmall,
                            color = LucidTextSubtle,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
                Text(
                    "${alarm.label}  ·  ${alarm.daysLabel()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LucidTextVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    BuiltinSoundCatalog.name(alarm.soundId),
                    style = MaterialTheme.typography.labelSmall,
                    color = LucidTextSubtle,
                )
            }
            LucidSwitch(checked = alarm.enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun AlarmDaySelector(selected: Set<Int>, onToggle: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { index, letter ->
            val day = index + 1
            val active = day in selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) LucidPrimary.copy(alpha = 0.18f) else LucidSurface2)
                    .border(
                        1.dp,
                        if (active) LucidPrimary.copy(alpha = 0.55f) else LucidBorder,
                        RoundedCornerShape(10.dp),
                    )
                    .clickable { onToggle(day) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    letter,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) LucidPrimarySoft else LucidTextVariant,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmEditorSheet(
    alarm: Alarm,
    settings: AppSettings,
    sounds: List<com.lemomation.lucidlogger.audio.BuiltinSound>,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit,
    onDelete: () -> Unit,
) {
    var draft by remember(alarm.id) { mutableStateOf(alarm) }
    var showTimePicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val preview = remember { SoundPreview(context) }
    val customSoundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            draft = draft.copy(
                soundId = BuiltinSounds.CUSTOM,
                customSoundUri = uri.toString(),
                customSoundName = displayName(context, uri) ?: "Custom alarm sound",
            )
        }
    }
    val playingId by preview.playingId.collectAsStateWithLifecycle()
    DisposableEffect(Unit) { onDispose { preview.stop() } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LucidSurface,
        scrimColor = Color.Black.copy(alpha = 0.62f),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 34.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(LucidTextSubtle.copy(alpha = 0.55f)),
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Edit alarm", style = MaterialTheme.typography.headlineSmall, color = LucidText, modifier = Modifier.weight(1f))
                IconButton(onClick = { preview.stop(); onDismiss() }) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = LucidTextVariant)
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(LucidSurface2)
                    .border(1.dp, LucidBorderStrong, RoundedCornerShape(14.dp))
                    .clickable { showTimePicker = true }
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    formatTime(draft.hour, draft.minute, settings.use24Hour),
                    style = MaterialTheme.typography.displaySmall,
                    color = LucidAmber,
                    fontWeight = FontWeight.Light,
                )
            }

            LucidInput(
                value = draft.label,
                onValueChange = { draft = draft.copy(label = it) },
                placeholder = "Alarm label",
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
            )

            Column {
                Text("Repeat", style = MaterialTheme.typography.labelSmall, color = LucidTextSubtle, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                AlarmDaySelector(selected = draft.days) { day ->
                    draft = draft.copy(days = if (day in draft.days) draft.days - day else draft.days + day)
                }
            }

            SettingRow("Alarm enabled", "Schedule this alarm") {
                LucidSwitch(checked = draft.enabled, onCheckedChange = { draft = draft.copy(enabled = it) })
            }
            HorizontalDivider(color = LucidBorder)
            SettingRow("Vibrate", "Gentle pattern while ringing") {
                LucidSwitch(checked = draft.vibrate, onCheckedChange = { draft = draft.copy(vibrate = it) })
            }
            HorizontalDivider(color = LucidBorder)
            SettingRow("Volume ramp", "Rise gradually from silence") {
                ValueStepper(
                    value = draft.volumeRampSeconds,
                    onDecrease = {
                        draft = draft.copy(volumeRampSeconds = (draft.volumeRampSeconds - 10).coerceAtLeast(0))
                    },
                    onIncrease = {
                        draft = draft.copy(volumeRampSeconds = (draft.volumeRampSeconds + 10).coerceAtMost(120))
                    },
                    suffix = "s",
                )
            }

            Column {
                Text("Sound", style = MaterialTheme.typography.labelSmall, color = LucidTextSubtle, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    sounds.forEach { sound ->
                        val selected = draft.soundId == sound.id
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) LucidSurface3 else LucidSurface2)
                                .border(1.dp, if (selected) LucidAmber.copy(alpha = 0.5f) else LucidBorder, RoundedCornerShape(10.dp))
                                .clickable { draft = draft.copy(soundId = sound.id) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) LucidAmber else LucidSurface3)
                                    .clickable { preview.toggle(sound.id, sound.rawResId) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    if (playingId == sound.id) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                    contentDescription = "Preview",
                                    tint = if (selected) LucidOnAmber else LucidTextVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Text(sound.name, style = MaterialTheme.typography.bodySmall, color = LucidText, modifier = Modifier.weight(1f))
                            if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = LucidAmber, modifier = Modifier.size(17.dp))
                        }
                    }
                }
                if (draft.soundId == BuiltinSounds.CUSTOM) {
                    Text(
                        draft.customSoundName ?: "Custom audio selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = LucidAmber,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                TextButton(
                    onClick = {
                        preview.stop()
                        customSoundLauncher.launch(arrayOf("audio/*"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Folder, contentDescription = null, tint = LucidTextVariant, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Choose custom audio", color = LucidTextVariant, style = MaterialTheme.typography.bodySmall)
                }
            }

            PrimaryButton(
                text = "Save alarm",
                onClick = { preview.stop(); onSave(draft) },
                modifier = Modifier.fillMaxWidth(),
                accent = true,
            )
            TextButton(onClick = { preview.stop(); onDelete() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = LucidError, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("Delete alarm", color = LucidError)
            }
        }
    }

    if (showTimePicker) {
        LucidTimePickerDialog(
            initialHour = draft.hour,
            initialMinute = draft.minute,
            use24Hour = settings.use24Hour,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                draft = draft.copy(hour = hour, minute = minute)
                showTimePicker = false
            },
        )
    }
}

@Composable
private fun AlarmPermissionsCard(
    context: Context,
    refreshKey: Int,
    onNotifications: () -> Unit,
    onExactAlarm: () -> Unit,
    onFullScreen: () -> Unit,
    onMic: () -> Unit,
    onBattery: () -> Unit,
) {
    val issues = remember(refreshKey) { permissionIssues(context) }
    if (issues.isEmpty()) return
    LucidCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = LucidAmber, modifier = Modifier.size(17.dp))
            Text("ALARM READINESS", style = MaterialTheme.typography.labelSmall, color = LucidAmber, letterSpacing = 0.9.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "A few permissions keep the alarm reliable on this device.",
            style = MaterialTheme.typography.bodySmall,
            color = LucidTextVariant,
        )
        Spacer(Modifier.height(8.dp))
        issues.forEach { issue ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(LucidAmber))
                Text(issue.label, style = MaterialTheme.typography.bodySmall, color = LucidText, modifier = Modifier.weight(1f))
                TextButton(
                    onClick = {
                        when (issue.type) {
                            PermissionIssueType.NOTIFICATIONS -> onNotifications()
                            PermissionIssueType.EXACT_ALARM -> onExactAlarm()
                            PermissionIssueType.FULL_SCREEN -> onFullScreen()
                            PermissionIssueType.MIC -> onMic()
                            PermissionIssueType.BATTERY -> onBattery()
                        }
                    },
                ) {
                    Text("Fix", color = LucidAmber, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

private enum class PermissionIssueType { NOTIFICATIONS, EXACT_ALARM, FULL_SCREEN, MIC, BATTERY }

private data class PermissionIssue(val type: PermissionIssueType, val label: String)

private fun permissionIssues(context: Context): List<PermissionIssue> {
    val issues = mutableListOf<PermissionIssue>()
    val notificationManager = NotificationManagerCompat.from(context)
    if (!notificationManager.areNotificationsEnabled()) {
        issues += PermissionIssue(PermissionIssueType.NOTIFICATIONS, "Notifications are disabled")
    }
    if (Build.VERSION.SDK_INT >= 31) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!alarmManager.canScheduleExactAlarms()) {
            issues += PermissionIssue(PermissionIssueType.EXACT_ALARM, "Exact alarms are not allowed")
        }
    }
    if (Build.VERSION.SDK_INT >= 34) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.canUseFullScreenIntent()) {
            issues += PermissionIssue(PermissionIssueType.FULL_SCREEN, "Full-screen alarm is not allowed")
        }
    }
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
        issues += PermissionIssue(PermissionIssueType.MIC, "Microphone access is needed")
    }
    val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    if (!power.isIgnoringBatteryOptimizations(context.packageName)) {
        issues += PermissionIssue(PermissionIssueType.BATTERY, "Battery optimization may delay alarms")
    }
    return issues
}

private fun openNotificationSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= 31) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }
    }
}

private fun openFullScreenSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= 34) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }
    }
}

private fun openBatterySettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }
}

private fun displayName(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
}.getOrNull()

private fun formatTime(hour: Int, minute: Int, use24Hour: Boolean): String {
    return if (use24Hour) {
        "%02d:%02d".format(hour, minute)
    } else {
        val h = if (hour % 12 == 0) 12 else hour % 12
        "%d:%02d %s".format(h, minute, if (hour < 12) "AM" else "PM")
    }
}

package com.lemomation.lucidlogger.ui.alarm

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.lemomation.lucidlogger.AppContainer
import com.lemomation.lucidlogger.alarm.AlarmService
import com.lemomation.lucidlogger.audio.AudioPlayerController
import com.lemomation.lucidlogger.audio.DreamRecorder
import com.lemomation.lucidlogger.data.model.Alarm
import com.lemomation.lucidlogger.data.model.AppSettings
import com.lemomation.lucidlogger.data.model.DreamNote
import com.lemomation.lucidlogger.data.model.DreamSource
import com.lemomation.lucidlogger.ui.components.AudioNotePlayer
import com.lemomation.lucidlogger.ui.components.LucidCard
import com.lemomation.lucidlogger.ui.components.LucidInput
import com.lemomation.lucidlogger.ui.components.PrimaryButton
import com.lemomation.lucidlogger.ui.components.ProgressBar
import com.lemomation.lucidlogger.ui.components.StatusPill
import com.lemomation.lucidlogger.ui.components.Waveform
import com.lemomation.lucidlogger.ui.theme.LucidAmber
import com.lemomation.lucidlogger.ui.theme.LucidBorder
import com.lemomation.lucidlogger.ui.theme.LucidCanvas
import com.lemomation.lucidlogger.ui.theme.LucidError
import com.lemomation.lucidlogger.ui.theme.LucidErrorContainer
import com.lemomation.lucidlogger.ui.theme.LucidOnAmber
import com.lemomation.lucidlogger.ui.theme.LucidPrimary
import com.lemomation.lucidlogger.ui.theme.LucidSurface
import com.lemomation.lucidlogger.ui.theme.LucidSurface2
import com.lemomation.lucidlogger.ui.theme.LucidSurface3
import com.lemomation.lucidlogger.ui.theme.LucidText
import com.lemomation.lucidlogger.ui.theme.LucidTextSubtle
import com.lemomation.lucidlogger.ui.theme.LucidTextVariant
import com.lemomation.lucidlogger.util.TextUtils
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.time.OffsetDateTime
import kotlin.math.max

@Composable
fun AlarmRingScreen(
    container: AppContainer,
    alarm: Alarm,
    settings: AppSettings,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recorder = remember { DreamRecorder(context) }
    val player = remember { AudioPlayerController() }
    val prefs = remember { context.getSharedPreferences("ring_state", Context.MODE_PRIVATE) }
    val snoozeKey = "snooze_${alarm.id}"

    var body by remember { mutableStateOf("") }
    var recording by remember { mutableStateOf(false) }
    var recordingStartedAt by remember { mutableLongStateOf(0L) }
    var elapsed by remember { mutableIntStateOf(0) }
    var ringSeconds by remember { mutableIntStateOf(0) }
    var amplitudes by remember { mutableStateOf<List<Int>>(emptyList()) }
    var pendingAudio by remember { mutableStateOf<File?>(null) }
    var pendingDuration by remember { mutableIntStateOf(0) }
    var pendingWaveform by remember { mutableStateOf<List<Int>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var snoozeCount by remember { mutableIntStateOf(prefs.getInt(snoozeKey, 0)) }
    var emergencyVisible by remember { mutableStateOf(false) }
    var emergencyFired by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableStateOf(0f) }
    var selectedMode by remember { mutableStateOf("voice") }

    val chars = TextUtils.writtenCharCount(body)
    val voiceEnough = pendingDuration >= settings.minVoiceSeconds
    val gateMet = voiceEnough || chars >= settings.minNoteChars
    val emergencyDelay = settings.emergencyDismissDelayMinutes * 60
    val snoozeAvailable = settings.snoozeEnabled && snoozeCount < settings.maxSnoozes

    fun finish() {
        prefs.edit().remove(snoozeKey).apply()
        AlarmService.stop(context)
        onFinished()
    }

    fun stopRecording(resumeWhenShort: Boolean = true) {
        if (!recording) return
        val result = recorder.stop(amplitudes)
        recording = false
        if (result != null) {
            player.pause()
            pendingAudio?.delete()
            pendingAudio = result.file
            pendingDuration = result.durationSeconds
            pendingWaveform = result.waveform
            elapsed = result.durationSeconds
            if (result.durationSeconds < settings.minVoiceSeconds) {
                if (resumeWhenShort) AlarmService.resume(context)
                error = "Keep speaking. ${settings.minVoiceSeconds - result.durationSeconds}s more to satisfy the voice gate."
            } else {
                error = null
            }
        } else {
            if (resumeWhenShort) AlarmService.resume(context)
            error = "The recording could not be saved. Try again or write the dream."
        }
    }

    fun beginRecording() {
        player.pause()
        if (recorder.start()) {
            AlarmService.pause(context)
            pendingAudio?.delete()
            pendingAudio = null
            pendingDuration = 0
            pendingWaveform = emptyList()
            amplitudes = emptyList()
            elapsed = 0
            error = null
            recordingStartedAt = SystemClock.elapsedRealtime()
            recording = true
            selectedMode = "voice"
        } else {
            error = "The microphone is unavailable. Write the dream instead."
        }
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) beginRecording()
        else error = "Microphone permission is needed. The written note still works."
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(1000)
            ringSeconds++
            if (!emergencyVisible && settings.emergencyDismissEnabled && ringSeconds >= emergencyDelay) {
                emergencyVisible = true
            }
        }
    }

    LaunchedEffect(recording) {
        while (recording) {
            elapsed = ((SystemClock.elapsedRealtime() - recordingStartedAt) / 1000).toInt()
            val amp = recorder.amplitude()
            val level = (amp / 500).coerceIn(4, 100)
            amplitudes = (amplitudes + level).takeLast(64)
            delay(200)
        }
    }

    LaunchedEffect(gateMet, saving, recording) {
        if (gateMet && !saving && !recording) {
            delay(60_000)
            if (!saving && !recording) {
                AlarmService.resume(context)
                error = "Save the dream to silence the alarm. The sound is back until the note is safe."
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (recorder.isRecording) recorder.cancel()
            player.release()
        }
    }

    BackHandler {
        error = "The alarm releases only when you save a dream or use the emergency exit."
    }

    fun saveDream() {
        if (saving) return
        if (recording) stopRecording(resumeWhenShort = false)
        val durationNow = if (pendingAudio != null) pendingDuration else 0
        val charsNow = TextUtils.writtenCharCount(body)
        if (durationNow < settings.minVoiceSeconds && charsNow < settings.minNoteChars) {
            error = "Record ${settings.minVoiceSeconds}s of voice or write ${settings.minNoteChars} characters first."
            AlarmService.resume(context)
            return
        }
        saving = true
        scope.launch {
            runCatching {
                container.dreams.saveDraft(
                    existing = null,
                    titleInput = "",
                    bodyInput = body,
                    audioTemp = pendingAudio,
                    audioDuration = if (pendingAudio != null) durationNow else null,
                    waveform = pendingWaveform,
                )
            }.onSuccess {
                finish()
            }.onFailure {
                saving = false
                error = "Could not save the dream. ${it.message ?: "Try again."}"
                AlarmService.resume(context)
            }
        }
    }

    fun snooze() {
        if (!snoozeAvailable) return
        prefs.edit().putInt(snoozeKey, snoozeCount + 1).apply()
        snoozeCount++
        AlarmService.stop(context)
        container.scheduler.scheduleSnooze(alarm, settings.snoozeMinutes)
        onFinished()
    }

    fun emergencyDismiss() {
        if (!emergencyVisible || emergencyFired) return
        emergencyFired = true
        AlarmService.stop(context)
        prefs.edit().remove(snoozeKey).apply()
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(LucidSurface, LucidCanvas, LucidCanvas),
                ),
            ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(40.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(Icons.Outlined.Bedtime, contentDescription = null, tint = LucidAmber, modifier = Modifier.size(16.dp))
                Text(
                    "LUCID LOGGER",
                    style = MaterialTheme.typography.labelSmall,
                    color = LucidAmber,
                    letterSpacing = 2.sp,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                formatTime(alarm.hour, alarm.minute, settings.use24Hour),
                style = MaterialTheme.typography.displayLarge,
                color = LucidText,
                fontWeight = FontWeight.Light,
                letterSpacing = (-1).sp,
            )
            Text(
                alarm.label.ifBlank { "Dream alarm" },
                style = MaterialTheme.typography.titleMedium,
                color = LucidTextVariant,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(LucidAmber.copy(alpha = 0.1f))
                    .border(1.dp, LucidAmber.copy(alpha = 0.25f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = LucidAmber, modifier = Modifier.size(13.dp))
                Text(
                    "THE ALARM RELEASES WHEN THE DREAM IS SAFE",
                    style = MaterialTheme.typography.labelSmall,
                    color = LucidAmber,
                )
            }

            Spacer(Modifier.height(26.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ModePill("Speak", selectedMode == "voice", Modifier.weight(1f)) { selectedMode = "voice" }
                ModePill("Write", selectedMode == "write", Modifier.weight(1f)) { selectedMode = "write" }
            }

            Spacer(Modifier.height(16.dp))

            if (selectedMode == "voice" || recording) {
                LucidCard(padding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
                            if (recording) {
                                CircularProgressIndicator(
                                    progress = {
                                        elapsed.toFloat() / settings.minVoiceSeconds.coerceAtLeast(1)
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                    color = LucidAmber,
                                    trackColor = LucidSurface3,
                                    strokeWidth = 3.dp,
                                )
                            } else {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .border(1.dp, LucidBorder, CircleShape),
                                )
                            }
                            Box(
                                Modifier
                                    .size(66.dp)
                                    .clip(CircleShape)
                                    .background(if (recording) LucidError.copy(alpha = 0.2f) else LucidAmber)
                                    .clickable {
                                        if (recording) stopRecording()
                                        else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                                            PackageManager.PERMISSION_GRANTED
                                        ) beginRecording()
                                        else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    if (recording) Icons.Filled.Stop else Icons.Filled.Mic,
                                    contentDescription = if (recording) "Stop recording" else "Record",
                                    tint = if (recording) LucidError else LucidOnAmber,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (recording) "Listening..." else if (pendingAudio != null) "Voice memo captured" else "Describe it out loud",
                                style = MaterialTheme.typography.titleLarge,
                                color = LucidText,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                if (recording) formatTimer(elapsed) + " / ${formatTimer(settings.minVoiceSeconds)}"
                                else "Sleepy words are enough. Say what you saw.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LucidTextVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (voiceEnough) "Voice gate met" else "${max(0, settings.minVoiceSeconds - pendingDuration)}s remaining",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (voiceEnough) LucidPrimary else LucidAmber,
                            )
                        }
                    }
                    if (recording || amplitudes.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Waveform(
                            values = amplitudes,
                            progress = 1f,
                            playedColor = if (recording) LucidAmber else LucidTextSubtle,
                            restColor = LucidTextSubtle,
                        )
                    }
                    if (pendingAudio != null && !recording) {
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Voice memo  ${TextUtils.formatDuration(pendingDuration)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = LucidTextVariant,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = {
                                player.pause()
                                pendingAudio?.delete()
                                pendingAudio = null
                                pendingDuration = 0
                                pendingWaveform = emptyList()
                                if (chars < settings.minNoteChars) AlarmService.resume(context)
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = null, tint = LucidTextSubtle, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("Remove", color = LucidTextSubtle, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        if (pendingAudio != null) {
                            AudioNotePlayer(
                                note = previewNote(pendingDuration, pendingWaveform),
                                file = pendingAudio,
                                waveform = pendingWaveform,
                                controller = player,
                            )
                        }
                    }
                }
            }

            if (selectedMode == "write" || recording) {
                Spacer(Modifier.height(14.dp))
                LucidInput(
                    value = body,
                    onValueChange = { body = it },
                    placeholder = "Write the dream before it dissolves. People, places, colors, feelings...",
                    minLines = 5,
                    textStyle = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(7.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "$chars / ${settings.minNoteChars} characters",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (chars >= settings.minNoteChars) LucidPrimary else LucidTextSubtle,
                    )
                    Text(
                        "Either gate is enough",
                        style = MaterialTheme.typography.labelSmall,
                        color = LucidTextSubtle,
                    )
                }
            }

            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LucidErrorContainer.copy(alpha = 0.25f))
                        .border(1.dp, LucidError.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = LucidError, modifier = Modifier.size(17.dp))
                    Text(
                        error.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = LucidError,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                text = if (saving) "Saving..." else "Save dream & silence alarm",
                onClick = { saveDream() },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
                accent = true,
                icon = Icons.Filled.GraphicEq,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (gateMet) "The gate is open. Save to release the alarm."
                else "The alarm will keep ringing until the gate is met.",
                style = MaterialTheme.typography.labelSmall,
                color = if (gateMet) LucidPrimary else LucidTextSubtle,
                textAlign = TextAlign.Center,
            )

            if (snoozeAvailable) {
                TextButton(onClick = { snooze() }, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Outlined.Snooze, contentDescription = null, tint = LucidTextVariant, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(
                        "Snooze ${settings.snoozeMinutes} min  (${snoozeCount}/${settings.maxSnoozes})",
                        color = LucidTextVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            if (emergencyVisible && settings.emergencyDismissEnabled) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Emergency exit",
                        style = MaterialTheme.typography.labelSmall,
                        color = LucidTextSubtle,
                    )
                    Text(
                        "Hold for 5 seconds to dismiss without recording.",
                        style = MaterialTheme.typography.labelSmall,
                        color = LucidTextSubtle,
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .border(1.dp, LucidError.copy(alpha = if (holdProgress > 0f) 0.6f else 0.22f), CircleShape)
                            .pointerInput(emergencyVisible, emergencyFired) {
                                detectTapGestures(
                                    onPress = {
                                        if (emergencyFired) return@detectTapGestures
                                        holdProgress = 0f
                                        coroutineScope {
                                            val timer = launch {
                                                val started = SystemClock.elapsedRealtime()
                                                while (isActive) {
                                                    val progress = ((SystemClock.elapsedRealtime() - started) / 5000f)
                                                        .coerceIn(0f, 1f)
                                                    holdProgress = progress
                                                    if (progress >= 1f) {
                                                        emergencyDismiss()
                                                        break
                                                    }
                                                    delay(50)
                                                }
                                            }
                                            tryAwaitRelease()
                                            timer.cancel()
                                        }
                                        holdProgress = 0f
                                    },
                                    onTap = { },
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Text(
                            if (holdProgress > 0f) "Keep holding..." else "Hold to dismiss",
                            style = MaterialTheme.typography.labelMedium,
                            color = LucidError,
                        )
                    }
                    if (holdProgress > 0f) {
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.fillMaxWidth(0.55f)) {
                            ProgressBar(fraction = holdProgress, color = LucidError, track = LucidSurface3)
                        }
                    }
                }
            } else if (settings.emergencyDismissEnabled) {
                Text(
                    "An emergency exit appears after ${settings.emergencyDismissDelayMinutes} minutes.",
                    style = MaterialTheme.typography.labelSmall,
                    color = LucidTextSubtle,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(34.dp))
        }
    }
}

@Composable
private fun ModePill(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(CircleShape)
            .background(if (selected) LucidAmber.copy(alpha = 0.14f) else LucidSurface2)
            .border(1.dp, if (selected) LucidAmber.copy(alpha = 0.42f) else LucidBorder, CircleShape)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) LucidAmber else LucidTextVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

private fun previewNote(duration: Int, waveform: List<Int>): DreamNote = DreamNote(
    id = "ring-draft",
    fileName = "ring-draft.md",
    title = "Voice memo",
    body = "",
    created = OffsetDateTime.now(),
    updated = null,
    tags = emptyList(),
    links = emptyList(),
    audioFile = "ring-draft.m4a",
    audioDuration = duration,
    waveform = waveform,
    lucid = false,
    source = DreamSource.VOICE,
)

private fun formatTimer(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

private fun formatTime(hour: Int, minute: Int, use24Hour: Boolean): String =
    if (use24Hour) "%02d:%02d".format(hour, minute)
    else {
        val suffix = if (hour < 12) "AM" else "PM"
        val display = if (hour % 12 == 0) 12 else hour % 12
        "%d:%02d %s".format(display, minute, suffix)
    }

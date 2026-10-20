package com.lemomation.lucidlogger.ui.journal

import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lemomation.lucidlogger.AppContainer
import com.lemomation.lucidlogger.ui.components.AudioNotePlayer
import com.lemomation.lucidlogger.audio.AudioPlayerController
import com.lemomation.lucidlogger.audio.DreamRecorder
import com.lemomation.lucidlogger.data.markdown.MarkdownCodec
import com.lemomation.lucidlogger.data.model.AppSettings
import com.lemomation.lucidlogger.data.model.DreamNote
import com.lemomation.lucidlogger.data.model.DreamSource
import com.lemomation.lucidlogger.ui.components.GhostButton
import com.lemomation.lucidlogger.ui.components.LucidCard
import com.lemomation.lucidlogger.ui.components.LucidInput
import com.lemomation.lucidlogger.ui.components.LucidTopBar
import com.lemomation.lucidlogger.ui.components.PrimaryButton
import com.lemomation.lucidlogger.ui.components.ProgressBar
import com.lemomation.lucidlogger.ui.components.SectionHeader
import com.lemomation.lucidlogger.ui.components.TagChip
import com.lemomation.lucidlogger.ui.components.Waveform
import com.lemomation.lucidlogger.ui.theme.LucidAmber
import com.lemomation.lucidlogger.ui.theme.LucidAmberDim
import com.lemomation.lucidlogger.ui.theme.LucidBorder
import com.lemomation.lucidlogger.ui.theme.LucidCanvas
import com.lemomation.lucidlogger.ui.theme.LucidError
import com.lemomation.lucidlogger.ui.theme.LucidPrimary
import com.lemomation.lucidlogger.ui.theme.LucidSurface
import com.lemomation.lucidlogger.ui.theme.LucidSurface2
import com.lemomation.lucidlogger.ui.theme.LucidSurface3
import com.lemomation.lucidlogger.ui.theme.LucidText
import com.lemomation.lucidlogger.ui.theme.LucidTextSubtle
import com.lemomation.lucidlogger.ui.theme.LucidTextVariant
import com.lemomation.lucidlogger.util.TextUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.time.OffsetDateTime
import kotlin.math.max

@Composable
fun DreamEditorScreen(
    container: AppContainer,
    existing: DreamNote?,
    autoStartRecording: Boolean,
    settings: AppSettings,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recorder = remember { DreamRecorder(context) }
    val player = remember { AudioPlayerController() }
    val playerState by player.state.collectAsStateWithLifecycle()

    var title by rememberSaveableFor(existing) { mutableStateOf(existing?.title.orEmpty()) }
    var body by rememberSaveableFor(existing) { mutableStateOf(existing?.body.orEmpty()) }
    var recording by remember { mutableStateOf(false) }
    var elapsed by remember { mutableIntStateOf(0) }
    var amplitudes by remember { mutableStateOf<List<Int>>(emptyList()) }
    var pendingAudio by remember { mutableStateOf<File?>(null) }
    var pendingDuration by remember { mutableIntStateOf(0) }
    var pendingWaveform by remember { mutableStateOf<List<Int>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var autoStarted by remember { mutableStateOf(false) }
    var recordingStartedAt by remember { mutableLongStateOf(0L) }

    val existingAudio = remember(existing) { existing?.let { container.dreams.audioFile(it) } }
    val previewFile = pendingAudio ?: existingAudio
    val previewDuration = if (pendingAudio != null) pendingDuration else existing?.audioDuration ?: 0
    val previewWave = if (pendingAudio != null) pendingWaveform else existing?.waveform.orEmpty()
    val chars = TextUtils.writtenCharCount(body)
    val hasAudioEnough = (if (pendingAudio != null) pendingDuration else existing?.audioDuration ?: 0) >= settings.minVoiceSeconds
    val canPassGate = chars >= settings.minNoteChars || hasAudioEnough
    val dirty = title != existing?.title.orEmpty() || body != existing?.body.orEmpty() || pendingAudio != null

    fun stopRecording() {
        if (!recording) return
        val result = recorder.stop(amplitudes)
        recording = false
        elapsed = result?.durationSeconds ?: elapsed
        if (result != null) {
            pendingAudio?.delete()
            pendingAudio = result.file
            pendingDuration = result.durationSeconds
            pendingWaveform = result.waveform
            error = null
        }
    }

    fun beginRecording() {
        player.pause()
        if (recorder.start()) {
            error = null
            pendingAudio?.delete()
            pendingAudio = null
            pendingDuration = 0
            pendingWaveform = emptyList()
            amplitudes = emptyList()
            elapsed = 0
            recordingStartedAt = SystemClock.elapsedRealtime()
            recording = true
        } else {
            error = "The microphone is unavailable. You can still write the dream."
        }
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            beginRecording()
        } else {
            error = "Microphone permission is needed to record a voice memo."
        }
    }

    LaunchedEffect(autoStartRecording) {
        if (autoStartRecording && !autoStarted) {
            autoStarted = true
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) beginRecording() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
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

    DisposableEffect(Unit) {
        onDispose {
            if (recorder.isRecording) recorder.cancel()
            player.release()
        }
    }

    fun save() {
        if (saving) return
        if (recording) stopRecording()
        val savedDuration = if (pendingAudio != null) pendingDuration else existing?.audioDuration ?: 0
        val savedChars = TextUtils.writtenCharCount(body)
        if (settings.enforceOnAllSaves && savedChars < settings.minNoteChars && savedDuration < settings.minVoiceSeconds) {
            error = "Reach ${settings.minVoiceSeconds}s of voice or ${settings.minNoteChars} characters before saving."
            return
        }
        saving = true
        scope.launch {
            runCatching {
                container.dreams.saveDraft(
                    existing = existing,
                    titleInput = title,
                    bodyInput = body,
                    audioTemp = pendingAudio,
                    audioDuration = if (pendingAudio != null) pendingDuration else null,
                    waveform = pendingWaveform,
                )
            }.onSuccess {
                onClose()
            }.onFailure {
                error = "Could not save this dream. ${it.message ?: "Try again."}"
                saving = false
            }
        }
    }

    BackHandler {
        if (dirty || recording) showDiscard = true else onClose()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(LucidCanvas)
            .imePadding(),
    ) {
        LucidTopBar(
            title = if (existing == null) "New dream" else "Edit dream",
            onBack = { if (dirty || recording) showDiscard = true else onClose() },
            actions = {
                TextButton(onClick = { save() }, enabled = !saving) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = LucidAmber, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(5.dp))
                    Text(if (saving) "Saving" else "Save", color = LucidAmber, fontWeight = FontWeight.Bold)
                }
            },
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            LucidInput(
                value = title,
                onValueChange = { title = it },
                placeholder = "Dream title (optional)",
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall,
            )

            LucidInput(
                value = body,
                onValueChange = { body = it },
                placeholder = "What do you remember? Fragments are enough...",
                minLines = 9,
                textStyle = MaterialTheme.typography.bodyLarge,
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "$chars characters",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (chars >= settings.minNoteChars) LucidPrimary else LucidTextSubtle,
                )
                Text(
                    "Minimum ${settings.minNoteChars}",
                    style = MaterialTheme.typography.labelSmall,
                    color = LucidTextSubtle,
                )
            }

            val metadata = remember(body) { MarkdownCodec.extractMetadata(body) }
            if (metadata.tags.isNotEmpty() || metadata.links.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    metadata.tags.take(4).forEach { TagChip(it) }
                    metadata.links.take(2).forEach { link ->
                        Text(
                            "[[$link]]",
                            style = MaterialTheme.typography.labelSmall,
                            color = LucidPrimary,
                            maxLines = 1,
                        )
                    }
                }
            }

            SectionHeader("Voice memo", Icons.Outlined.GraphicEq)

            LucidCard(padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (recording) LucidError.copy(alpha = 0.18f) else LucidAmber.copy(alpha = 0.14f))
                            .border(1.dp, if (recording) LucidError.copy(alpha = 0.5f) else LucidAmber.copy(alpha = 0.38f), CircleShape)
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
                            contentDescription = if (recording) "Stop recording" else "Start recording",
                            tint = if (recording) LucidError else LucidAmber,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            when {
                                recording -> "Recording  ${formatClock(elapsed)}"
                                pendingAudio != null -> "New voice memo  ${TextUtils.formatDuration(pendingDuration)}"
                                existingAudio != null -> "Saved voice memo  ${TextUtils.formatDuration(existing?.audioDuration ?: 0)}"
                                else -> "Speak while the dream is fresh"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = LucidText,
                        )
                        Text(
                            if (recording) "Tap stop when you have reached ${settings.minVoiceSeconds}s."
                            else "Minimum ${settings.minVoiceSeconds}s for the alarm gate.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LucidTextSubtle,
                        )
                    }
                    if (pendingAudio != null) {
                        TextButton(onClick = {
                            player.pause()
                            pendingAudio?.delete()
                            pendingAudio = null
                            pendingDuration = 0
                            pendingWaveform = emptyList()
                        }) {
                            Text("Remove", color = LucidTextVariant, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (recording || amplitudes.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Waveform(
                        values = amplitudes,
                        progress = 1f,
                        playedColor = if (recording) LucidError else LucidAmberDim,
                        restColor = LucidTextSubtle,
                    )
                }

                val audioSeconds = if (pendingAudio != null) pendingDuration else existing?.audioDuration ?: 0
                if (audioSeconds > 0) {
                    Spacer(Modifier.height(8.dp))
                    ProgressBar(
                        fraction = audioSeconds.toFloat() / settings.minVoiceSeconds.coerceAtLeast(1),
                        color = if (audioSeconds >= settings.minVoiceSeconds) LucidPrimary else LucidAmber,
                    )
                }
            }

            if (previewFile != null && !recording) {
                AudioNotePlayer(
                    note = previewNote(existing, previewDuration, previewWave),
                    file = previewFile,
                    waveform = previewWave,
                    controller = player,
                )
                if (playerState.filePath == previewFile.absolutePath && playerState.isPlaying) {
                    Text(
                        "Playing the memo. Tap again to pause.",
                        style = MaterialTheme.typography.labelSmall,
                        color = LucidTextSubtle,
                    )
                }
            }

            if (error != null) {
                Text(
                    error.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = LucidError,
                )
            }

            if (settings.enforceOnAllSaves && !canPassGate) {
                LucidCard(padding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
                    Text(
                        "This vault requires a real memory before saving.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LucidAmber,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Write ${max(0, settings.minNoteChars - chars)} more characters or record " +
                            "${max(0, settings.minVoiceSeconds - (if (pendingAudio != null) pendingDuration else existing?.audioDuration ?: 0))}s more voice.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LucidTextVariant,
                    )
                }
            }

            PrimaryButton(
                text = if (saving) "Saving dream..." else "Save dream",
                onClick = { save() },
                enabled = !saving,
                accent = true,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Outlined.EditNote,
            )

            if (existing != null) {
                GhostButton(
                    text = "Delete this dream",
                    onClick = { showDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Filled.Delete,
                )
            }
        }
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            containerColor = LucidSurface2,
            title = { Text("Discard this dream?", color = LucidText) },
            text = {
                Text(
                    "The words and voice memo on this screen will be lost.",
                    color = LucidTextVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (recorder.isRecording) recorder.cancel()
                    showDiscard = false
                    onClose()
                }) { Text("Discard", color = LucidError) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscard = false }) { Text("Keep editing", color = LucidTextVariant) }
            },
        )
    }

    if (showDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            containerColor = LucidSurface2,
            title = { Text("Delete this dream?", color = LucidText) },
            text = { Text("This removes the note and its audio from Lucid Logger.", color = LucidTextVariant) },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    scope.launch {
                        container.dreams.delete(existing)
                        onClose()
                    }
                }) { Text("Delete", color = LucidError) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Cancel", color = LucidTextVariant) }
            },
        )
    }
}

@Composable
private fun rememberSaveableFor(existing: DreamNote?, init: () -> androidx.compose.runtime.MutableState<String>) =
    androidx.compose.runtime.saveable.rememberSaveable(existing?.id) { init() }

private fun previewNote(existing: DreamNote?, duration: Int, waveform: List<Int>): DreamNote =
    existing?.copy(audioDuration = duration, waveform = waveform)
        ?: DreamNote(
            id = "draft",
            fileName = "draft.md",
            title = "Voice memo",
            body = "",
            created = OffsetDateTime.now(),
            updated = null,
            tags = emptyList(),
            links = emptyList(),
            audioFile = "draft.m4a",
            audioDuration = duration,
            waveform = waveform,
            lucid = false,
            source = DreamSource.VOICE,
        )

private fun formatClock(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

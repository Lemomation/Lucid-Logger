package com.lemomation.lucidlogger.data.store

import com.lemomation.lucidlogger.data.markdown.MarkdownCodec
import com.lemomation.lucidlogger.data.markdown.WikiLinker
import com.lemomation.lucidlogger.data.model.DreamNote
import com.lemomation.lucidlogger.data.model.DreamSource
import com.lemomation.lucidlogger.data.prefs.SettingsRepository
import com.lemomation.lucidlogger.util.TextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private val AUDIO_EMBED_REGEX = Regex("!\\[\\[[^]]+\\.(?:m4a|mp3|wav|ogg|aac|flac)]]", RegexOption.IGNORE_CASE)

class DreamRepository(
    private val store: DreamStore,
    private val settings: SettingsRepository,
) {
    private val mutex = Mutex()
    private val _notes = MutableStateFlow<List<DreamNote>>(emptyList())
    val notes: StateFlow<List<DreamNote>> = _notes.asStateFlow()

    var autoExporter: (suspend (DreamNote) -> Unit)? = null
    var autoDeleteExporter: (suspend (DreamNote) -> Unit)? = null

    suspend fun refresh() = withContext(Dispatchers.IO) {
        _notes.value = store.list()
    }

    fun noteById(id: String): DreamNote? = _notes.value.firstOrNull { it.id == id }

    fun audioFile(note: DreamNote): File? = store.audioFile(note)

    fun storageBytes(): Long = store.storageBytes()

    fun knownEntities(): List<String> = _notes.value
        .flatMap { it.links }
        .distinctBy { it.lowercase() }
        .sorted()

    suspend fun saveDraft(
        existing: DreamNote?,
        titleInput: String,
        bodyInput: String,
        audioTemp: File?,
        audioDuration: Int?,
        waveform: List<Int>,
    ): DreamNote = mutex.withLock {
        withContext(Dispatchers.IO) {
            val currentSettings = settings.current()
            val now = OffsetDateTime.now()
            val created = existing?.created ?: now
            var body = bodyInput.trim()
            if (currentSettings.autoWikilinks && body.isNotBlank()) {
                body = WikiLinker.autoLink(body, knownEntities())
            }
            val title = titleInput.trim().ifBlank {
                TextUtils.autoTitle(
                    body,
                    existing?.title ?: "Dream at ${now.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                )
            }
            if (audioTemp != null && body.isBlank()) {
                body = TextUtils.VOICE_PLACEHOLDER
            }

            val base = existing?.fileName?.removeSuffix(".md")
                ?: store.uniqueBase(created, title)
            var audioFile = existing?.audioFile
            var duration = existing?.audioDuration
            var wave = existing?.waveform.orEmpty()

            if (audioTemp != null) {
                body = body.replace(AUDIO_EMBED_REGEX, "").trim()
                val newAudio = store.storeAudio(audioTemp, base)
                if (existing?.audioFile != null && existing.audioFile != newAudio) {
                    store.audioFile(existing.audioFile)?.delete()
                }
                audioFile = newAudio
                duration = audioDuration
                wave = waveform.take(64)
                body = buildString {
                    append(body)
                    if (isNotBlank()) appendLine().appendLine()
                    append("![[$newAudio]]")
                }.trim()
            } else if (audioFile != null && !body.contains(audioFile)) {
                body = buildString {
                    append(body)
                    if (isNotBlank()) appendLine().appendLine()
                    append("![[$audioFile]]")
                }.trim()
            }

            if (TextUtils.hasWrittenText(body)) {
                body = body.replace(TextUtils.VOICE_PLACEHOLDER, "").trim()
            }
            val metadata = MarkdownCodec.extractMetadata(body)
            val source = when {
                audioFile != null && TextUtils.hasWrittenText(body) -> DreamSource.MIXED
                audioFile != null -> DreamSource.VOICE
                else -> DreamSource.NOTE
            }
            val note = DreamNote(
                id = existing?.id ?: base,
                fileName = existing?.fileName ?: "$base.md",
                title = title,
                body = body,
                created = created,
                updated = now,
                tags = metadata.tags,
                links = metadata.links,
                audioFile = audioFile,
                audioDuration = duration,
                waveform = wave,
                lucid = metadata.lucid || existing?.lucid == true,
                source = source,
            )
            store.write(note)
            _notes.value = store.list()
            autoExporter?.invoke(note)
            note
        }
    }

    suspend fun delete(note: DreamNote) = mutex.withLock {
        withContext(Dispatchers.IO) {
            store.delete(note)
            _notes.value = store.list()
            autoDeleteExporter?.invoke(note)
        }
    }

    suspend fun deleteAll() = mutex.withLock {
        withContext(Dispatchers.IO) {
            store.deleteAll()
            _notes.value = emptyList()
        }
    }
}

package com.lemomation.lucidlogger.data.store

import android.content.Context
import com.lemomation.lucidlogger.data.markdown.MarkdownCodec
import com.lemomation.lucidlogger.data.model.DreamNote
import com.lemomation.lucidlogger.util.TextUtils
import java.io.File
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class DreamStore(context: Context) {
    val root: File = File(context.filesDir, "dreams").apply { mkdirs() }
    val audioDir: File = File(root, "audio").apply { mkdirs() }

    fun list(): List<DreamNote> = root.listFiles()
        ?.asSequence()
        ?.filter { it.isFile && it.extension.equals("md", ignoreCase = true) }
        ?.mapNotNull { file -> runCatching { MarkdownCodec.decode(file.readText(), file.name) }.getOrNull() }
        ?.sortedByDescending { it.created.toInstant() }
        ?.toList()
        ?: emptyList()

    fun read(fileName: String): DreamNote? {
        val file = File(root, fileName)
        if (!file.isFile) return null
        return runCatching { MarkdownCodec.decode(file.readText(), file.name) }.getOrNull()
    }

    fun uniqueBase(created: OffsetDateTime, title: String): String {
        val stamp = created.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"))
        val slug = TextUtils.slugify(title)
        var base = "${stamp}_$slug"
        var counter = 2
        while (File(root, "$base.md").exists()) {
            base = "${stamp}_${slug}-$counter"
            counter++
        }
        return base
    }

    fun storeAudio(temp: File, base: String): String {
        val extension = temp.extension.ifBlank { "m4a" }
        var target = File(audioDir, "$base.$extension")
        var counter = 2
        while (target.exists()) {
            target = File(audioDir, "$base-$counter.$extension")
            counter++
        }
        temp.copyTo(target, overwrite = false)
        return "audio/${target.name}"
    }

    fun write(note: DreamNote) {
        val file = File(root, note.fileName)
        file.writeText(MarkdownCodec.encode(note))
    }

    fun delete(note: DreamNote) {
        File(root, note.fileName).delete()
        note.audioFile?.let { relative ->
            resolve(relative)?.delete()
        }
    }

    fun deleteAll() {
        root.listFiles()?.forEach { file ->
            if (file.isFile) file.delete()
        }
        audioDir.listFiles()?.forEach { it.delete() }
    }

    fun audioFile(note: DreamNote): File? =
        note.audioFile?.let { resolve(it) }?.takeIf { it.isFile }

    fun audioFile(relative: String): File? = resolve(relative)?.takeIf { it.isFile }

    fun resolve(relative: String): File? {
        val normalized = relative.replace('\\', '/').removePrefix("/")
        if (normalized.contains("..")) return null
        return File(root, normalized)
    }

    fun storageBytes(): Long {
        var total = 0L
        root.walkTopDown().filter { it.isFile }.forEach { total += it.length() }
        return total
    }

    fun exportedNotes(): List<DreamNote> = list()
}

package com.lemomation.lucidlogger.data.export

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.lemomation.lucidlogger.data.model.DreamNote
import com.lemomation.lucidlogger.data.store.DreamStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExportManager(
    private val context: Context,
    private val store: DreamStore,
) {
    private val rootFolderName = "Lucid Logger"
    private val audioFolderName = "audio"

    suspend fun exportAll(treeUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val root = openExportRoot(treeUri) ?: error("Could not open export folder")
            val notes = store.list()
            notes.forEach { note -> exportNoteTo(root, note) }
            notes.size
        }
    }

    suspend fun exportNote(treeUri: Uri, note: DreamNote): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val root = openExportRoot(treeUri) ?: error("Could not open export folder")
            exportNoteTo(root, note)
        }
    }

    suspend fun removeExported(treeUri: Uri, note: DreamNote): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val root = openExportRoot(treeUri) ?: return@runCatching
            root.findFile(note.fileName)?.delete()
            note.audioFile?.let { audio ->
                root.findFile(audioFolderName)?.findFile(audio.substringAfterLast('/'))?.delete()
            }
            Unit
        }
    }

    suspend fun writeBackup(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { raw ->
                ZipOutputStream(BufferedOutputStream(raw)).use { zip ->
                    store.root.walkTopDown()
                        .filter { it.isFile }
                        .forEach { file ->
                            val relative = file.relativeTo(store.root).invariantSeparatorsPath
                            zip.putNextEntry(ZipEntry("LucidLogger/$relative"))
                            file.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                }
            } ?: error("Could not open backup file")
        }
    }

    private fun openExportRoot(treeUri: Uri): DocumentFile? {
        val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        return tree.findFile(rootFolderName)
            ?: tree.createDirectory(rootFolderName)
    }

    private fun exportNoteTo(root: DocumentFile, note: DreamNote) {
        val existing = root.findFile(note.fileName)
        val file = existing ?: root.createFile("text/markdown", note.fileName) ?: error("Could not create note file")
        context.contentResolver.openOutputStream(file.uri, "wt")?.use { out ->
            out.write(com.lemomation.lucidlogger.data.markdown.MarkdownCodec.encode(note).toByteArray())
        } ?: error("Could not write note file")

        val audio = store.audioFile(note) ?: return
        val audioDir = root.findFile(audioFolderName) ?: root.createDirectory(audioFolderName) ?: return
        val audioName = audio.name
        val audioDoc = audioDir.findFile(audioName)
            ?: audioDir.createFile("audio/mp4", audioName)
            ?: return
        context.contentResolver.openOutputStream(audioDoc.uri)?.use { out ->
            audio.inputStream().use { it.copyTo(out) }
        }
    }
}

package com.lemomation.lucidlogger.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import java.io.File

data class RecordingResult(
    val file: File,
    val durationSeconds: Int,
    val waveform: List<Int>,
)

class DreamRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAt = 0L

    val isRecording: Boolean get() = recorder != null

    fun start(): Boolean {
        if (recorder != null) return true
        val dir = File(context.cacheDir, "recordings").apply { mkdirs() }
        val file = File(dir, "rec_${System.currentTimeMillis()}.m4a")
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        return runCatching {
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44_100)
                setAudioEncodingBitRate(96_000)
                setAudioChannels(1)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            outputFile = file
            startedAt = SystemClock.elapsedRealtime()
            true
        }.getOrElse {
            runCatching { mediaRecorder.release() }
            file.delete()
            false
        }
    }

    fun amplitude(): Int = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)

    fun stop(waveform: List<Int>): RecordingResult? {
        val mediaRecorder = recorder ?: return null
        val file = outputFile
        val elapsed = ((SystemClock.elapsedRealtime() - startedAt) / 1000).toInt()
        recorder = null
        outputFile = null
        val stopped = runCatching {
            mediaRecorder.stop()
            true
        }.getOrElse {
            file?.delete()
            false
        }
        runCatching { mediaRecorder.release() }
        if (!stopped || file == null || !file.exists()) return null
        val duration = durationOf(file) ?: elapsed
        val wave = if (waveform.isEmpty()) {
            List(32) { (6 + (it * 7) % 28) }
        } else {
            waveform.takeLast(48)
        }
        return RecordingResult(file, duration.coerceAtLeast(1), wave)
    }

    fun cancel() {
        val mediaRecorder = recorder
        recorder = null
        val file = outputFile
        outputFile = null
        runCatching { mediaRecorder?.stop() }
        runCatching { mediaRecorder?.release() }
        file?.delete()
    }

    private fun durationOf(file: File): Int? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            val millis = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: return@runCatching null
            (millis / 1000).toInt()
        } finally {
            retriever.release()
        }
    }.getOrNull()
}

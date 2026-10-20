package com.lemomation.lucidlogger.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class AudioPlaybackState(
    val filePath: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
)

class AudioPlayerController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var player: MediaPlayer? = null
    private var ticker: Job? = null
    private val _state = MutableStateFlow(AudioPlaybackState())
    val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

    fun toggle(file: File) {
        val existing = player
        if (existing != null && _state.value.filePath == file.absolutePath) {
            if (existing.isPlaying) {
                runCatching { existing.pause() }
                _state.value = _state.value.copy(isPlaying = false)
            } else {
                runCatching { existing.start() }
                _state.value = _state.value.copy(isPlaying = true)
                ensureTicker()
            }
            return
        }
        play(file)
    }

    fun play(file: File) {
        releasePlayer()
        runCatching {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    _state.value = _state.value.copy(
                        isPlaying = false,
                        positionMs = _state.value.durationMs,
                    )
                }
                start()
            }
            player = mp
            _state.value = AudioPlaybackState(
                filePath = file.absolutePath,
                isPlaying = true,
                positionMs = 0,
                durationMs = mp.duration.coerceAtLeast(0),
            )
            ensureTicker()
        }.onFailure {
            _state.value = AudioPlaybackState()
        }
    }

    fun seekTo(positionMs: Int) {
        player?.let { mp ->
            runCatching { mp.seekTo(positionMs.coerceIn(0, mp.duration.coerceAtLeast(0))) }
            _state.value = _state.value.copy(positionMs = positionMs)
        }
    }

    fun pause() {
        runCatching { player?.pause() }
        _state.value = _state.value.copy(isPlaying = false)
    }

    fun release() {
        ticker?.cancel()
        ticker = null
        releasePlayer()
        _state.value = AudioPlaybackState()
        scope.cancel()
    }

    private fun ensureTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                val mp = player
                if (mp != null) {
                    val playing = runCatching { mp.isPlaying }.getOrDefault(false)
                    _state.value = _state.value.copy(
                        isPlaying = playing,
                        positionMs = runCatching { mp.currentPosition }.getOrDefault(0),
                        durationMs = runCatching { mp.duration }.getOrDefault(0),
                    )
                }
                delay(200)
            }
        }
    }

    private fun releasePlayer() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
    }
}

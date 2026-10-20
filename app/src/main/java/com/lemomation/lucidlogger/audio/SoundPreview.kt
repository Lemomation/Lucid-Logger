package com.lemomation.lucidlogger.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.annotation.RawRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SoundPreview(private val context: Context) {
    private var player: MediaPlayer? = null
    private val _playingId = MutableStateFlow<String?>(null)
    val playingId: StateFlow<String?> = _playingId.asStateFlow()

    fun toggle(id: String, @RawRes resId: Int) {
        if (_playingId.value == id) {
            stop()
            return
        }
        stop()
        runCatching {
            val mp = MediaPlayer.create(context, resId) ?: return
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            mp.setVolume(0.6f, 0.6f)
            mp.setOnCompletionListener { stop() }
            mp.start()
            player = mp
            _playingId.value = id
        }.onFailure { stop() }
    }

    fun stop() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        _playingId.value = null
    }
}

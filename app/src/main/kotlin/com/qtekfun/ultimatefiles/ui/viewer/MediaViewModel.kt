package com.qtekfun.ultimatefiles.ui.viewer

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.qtekfun.ultimatefiles.domain.usecase.PlaybackMath
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerState(
    val playing: Boolean = false,
    val positionMillis: Long = 0,
    val durationMillis: Long = 0,
    /** Width over height of the picture; zero for audio. */
    val videoRatio: Float = 0f,
    val speed: Float = 1f,
    val repeat: Boolean = false,
    val muted: Boolean = false,
    val failed: Boolean = false,
)

private const val PROGRESS_MILLIS = 250L

class MediaViewModel(application: Application, uri: Uri) : ViewModel() {
    val player: ExoPlayer = ExoPlayer.Builder(application).build()

    private val _state = MutableStateFlow(PlayerState())
    val state = _state.asStateFlow()

    init {
        player.setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
            true,
        )
        player.addListener(
            object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) = refresh()

                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    val ratio = if (videoSize.height == 0) {
                        0f
                    } else {
                        videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                    }
                    _state.update { it.copy(videoRatio = ratio) }
                }

                override fun onPlayerError(error: PlaybackException) {
                    _state.update { it.copy(failed = true) }
                }
            },
        )
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.play()
        viewModelScope.launch {
            while (isActive) {
                if (player.isPlaying) refresh()
                delay(PROGRESS_MILLIS)
            }
        }
    }

    private fun refresh() = _state.update {
        it.copy(
            // Buffering counts as playing, so the button does not flicker.
            playing = player.playWhenReady && player.playbackState != Player.STATE_ENDED,
            positionMillis = player.currentPosition,
            durationMillis = player.duration.takeIf { d -> d != C.TIME_UNSET } ?: 0,
            speed = player.playbackParameters.speed,
            repeat = player.repeatMode == Player.REPEAT_MODE_ONE,
            muted = player.volume == 0f,
        )
    }

    fun togglePlay() {
        if (player.playbackState == Player.STATE_ENDED) {
            player.seekTo(0)
            player.play()
        } else if (player.playWhenReady) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun pause() = player.pause()

    fun seekTo(millis: Long) {
        player.seekTo(millis)
        refresh()
    }

    fun skip(delta: Long) = seekTo(PlaybackMath.skip(player.currentPosition, delta, _state.value.durationMillis))

    fun setSpeed(speed: Float) = player.setPlaybackSpeed(speed)

    fun toggleRepeat() {
        player.repeatMode = if (player.repeatMode == Player.REPEAT_MODE_ONE) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
    }

    fun toggleMute() {
        player.volume = if (player.volume == 0f) 1f else 0f
        refresh()
    }

    override fun onCleared() = player.release()
}

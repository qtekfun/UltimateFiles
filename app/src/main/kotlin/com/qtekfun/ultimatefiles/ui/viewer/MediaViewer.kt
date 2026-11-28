package com.qtekfun.ultimatefiles.ui.viewer

import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qtekfun.ultimatefiles.R
import com.qtekfun.ultimatefiles.domain.usecase.PlaybackMath
import kotlinx.coroutines.delay

private const val CONTROLS_TIMEOUT_MILLIS = 3500L

/** Video or audio with the controls docked below; tapping a video hides and shows them. */
@Composable
internal fun MediaViewer(viewModel: MediaViewModel, modifier: Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.failed) {
        ViewerMessage(R.string.viewer_error, modifier)
        return
    }
    val video = state.videoRatio > 0f
    var controls by rememberSaveable { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.pause() }
    val view = LocalView.current
    DisposableEffect(state.playing) {
        view.keepScreenOn = state.playing
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(controls, state.playing, video) {
        if (video && controls && state.playing) {
            delay(CONTROLS_TIMEOUT_MILLIS)
            controls = false
        }
    }

    Column(modifier) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(if (video) Color.Black else Color.Transparent)
                .clickable(enabled = video) { controls = !controls },
            Alignment.Center,
        ) {
            // The surface exists from the start: the player only reports a picture size once it has somewhere to draw.
            AndroidView(
                factory = { SurfaceView(it).also(viewModel.player::setVideoSurfaceView) },
                onRelease = { viewModel.player.clearVideoSurfaceView(it) },
                modifier = if (video) Modifier.aspectRatio(state.videoRatio) else Modifier.size(1.dp),
            )
            if (!video) {
                Icon(
                    Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(128.dp),
                )
            }
        }
        if (controls || !video || !state.playing) PlayerControls(viewModel, state)
    }
}

@Composable
private fun PlayerControls(viewModel: MediaViewModel, state: PlayerState) {
    // While dragging, the thumb follows the finger and not the player.
    var dragged by remember { mutableStateOf<Float?>(null) }
    var speeds by remember { mutableStateOf(false) }
    val duration = state.durationMillis
    val position = dragged?.toLong() ?: state.positionMillis
    val seekLabel = stringResource(R.string.player_seek)
    Surface(tonalElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Slider(
                value = position.toFloat().coerceIn(0f, duration.toFloat()),
                onValueChange = { dragged = it },
                onValueChangeFinished = {
                    dragged?.let { viewModel.seekTo(it.toLong()) }
                    dragged = null
                },
                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                enabled = duration > 0,
                modifier = Modifier.semantics { contentDescription = seekLabel },
            )
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(PlaybackMath.time(position), style = MaterialTheme.typography.labelMedium)
                Text(PlaybackMath.time(duration), style = MaterialTheme.typography.labelMedium)
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
                Box {
                    TextButton(onClick = { speeds = true }) { Text(stringResource(R.string.player_speed, state.speed.toString())) }
                    DropdownMenu(expanded = speeds, onDismissRequest = { speeds = false }) {
                        PlaybackMath.SPEEDS.forEach { speed ->
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.player_speed, speed.toString())) },
                                onClick = {
                                    viewModel.setSpeed(speed)
                                    speeds = false
                                },
                            )
                        }
                    }
                }
                IconButton(onClick = { viewModel.skip(-PlaybackMath.SKIP_MILLIS) }) {
                    Icon(Icons.Filled.Replay10, contentDescription = stringResource(R.string.player_back_10))
                }
                FilledIconButton(onClick = viewModel::togglePlay, modifier = Modifier.size(56.dp)) {
                    Icon(
                        if (state.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(if (state.playing) R.string.player_pause else R.string.player_play),
                    )
                }
                IconButton(onClick = { viewModel.skip(PlaybackMath.SKIP_MILLIS) }) {
                    Icon(Icons.Filled.Forward10, contentDescription = stringResource(R.string.player_forward_10))
                }
                IconToggleButton(checked = state.repeat, onCheckedChange = { viewModel.toggleRepeat() }) {
                    Icon(Icons.Filled.RepeatOne, contentDescription = stringResource(R.string.player_repeat))
                }
                IconToggleButton(checked = state.muted, onCheckedChange = { viewModel.toggleMute() }) {
                    Icon(
                        if (state.muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = stringResource(R.string.player_mute),
                    )
                }
            }
        }
    }
}

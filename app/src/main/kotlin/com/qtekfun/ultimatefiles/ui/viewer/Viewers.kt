package com.qtekfun.ultimatefiles.ui.viewer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.qtekfun.ultimatefiles.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface Loaded<out T> {
    data object Loading : Loaded<Nothing>
    data class Ready<T>(val value: T) : Loaded<T>
    data class Failed(val message: String?) : Loaded<Nothing>
}

private const val MAX_IMAGE_SIDE = 2048

@Composable
internal fun ImageViewer(uri: Uri, modifier: Modifier) {
    val resolver = LocalContext.current.contentResolver
    val state by produceState<Loaded<Bitmap>>(Loaded.Loading, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_IMAGE_SIDE) sample *= 2
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                bitmap ?: error("Cannot decode image")
            }.fold({ Loaded.Ready(it) }, { Loaded.Failed(it.message) })
        }
    }
    when (val current = state) {
        Loaded.Loading -> Box(modifier, Alignment.Center) { CircularProgressIndicator() }
        is Loaded.Failed -> ViewerMessage(R.string.viewer_error, modifier, current.message)
        is Loaded.Ready -> {
            var scale by remember { mutableFloatStateOf(1f) }
            var offsetX by remember { mutableFloatStateOf(0f) }
            var offsetY by remember { mutableFloatStateOf(0f) }
            Image(
                bitmap = current.value.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = modifier
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 8f)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    }
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY),
            )
        }
    }
}

@Composable
internal fun MediaViewer(uri: Uri, modifier: Modifier) {
    var failed by remember { mutableStateOf(false) }
    if (failed) {
        ViewerMessage(R.string.viewer_error, modifier)
        return
    }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            VideoView(context).apply {
                val controller = MediaController(context)
                controller.setAnchorView(this)
                setMediaController(controller)
                setVideoURI(uri)
                setOnErrorListener { _, _, _ ->
                    failed = true
                    true
                }
                setOnPreparedListener { start() }
            }
        },
        onRelease = { it.stopPlayback() },
    )
}

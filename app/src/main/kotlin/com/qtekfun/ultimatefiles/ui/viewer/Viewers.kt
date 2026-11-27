package com.qtekfun.ultimatefiles.ui.viewer

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.qtekfun.ultimatefiles.R

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

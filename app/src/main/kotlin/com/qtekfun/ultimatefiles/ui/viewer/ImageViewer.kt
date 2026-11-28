package com.qtekfun.ultimatefiles.ui.viewer

import android.graphics.Bitmap
import android.graphics.Matrix
import android.text.format.Formatter
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qtekfun.ultimatefiles.R
import com.qtekfun.ultimatefiles.core.model.FileItem
import com.qtekfun.ultimatefiles.domain.usecase.Zoom
import java.text.DateFormat
import java.util.Date

private const val MAX_IMAGE_ZOOM = 8f
private const val DOUBLE_TAP_IMAGE_ZOOM = 2.5f

/** The images of the opened file's folder, one per page. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImageGalleryScreen(name: String, viewModel: ImageViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ready = state as? GalleryState.Ready
    val pager = rememberPagerState(initialPage = ready?.start ?: 0) { ready?.images?.size ?: 0 }
    val loaded = remember { mutableStateMapOf<String, LoadedImage>() }
    var info by rememberSaveable { mutableStateOf(false) }
    var positioned by rememberSaveable { mutableStateOf(false) }
    val current = ready?.images?.getOrNull(pager.currentPage)

    LaunchedEffect(ready) {
        if (ready != null && !positioned) {
            pager.scrollToPage(ready.start)
            positioned = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(current?.name ?: name, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        bottomBar = {
            if (current != null) {
                BottomAppBar {
                    Text(
                        stringResource(R.string.image_position, pager.currentPage + 1, ready.images.size),
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    )
                    IconButton(onClick = { viewModel.rotate(current.path) }) {
                        Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = stringResource(R.string.image_rotate))
                    }
                    IconButton(onClick = { info = true }) {
                        Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.image_info))
                    }
                }
            }
        },
    ) { padding ->
        val content = Modifier.padding(padding).fillMaxSize()
        when (state) {
            GalleryState.Loading -> Box(content, Alignment.Center) { CircularProgressIndicator() }
            GalleryState.Failed -> ViewerMessage(R.string.viewer_error, content)
            is GalleryState.Ready -> HorizontalPager(state = pager, modifier = content.clipToBounds()) { page ->
                val item = ready!!.images[page]
                ImagePage(
                    viewModel = viewModel,
                    item = item,
                    settled = pager.settledPage == page,
                    onLoaded = { loaded[item.path] = it },
                )
            }
        }
    }

    if (info && current != null) {
        val image = loaded[current.path]
        ImageInfo(current, image?.width, image?.height, image?.taken, image?.camera) { info = false }
    }
}

@Composable
private fun ImagePage(
    viewModel: ImageViewModel,
    item: FileItem,
    settled: Boolean,
    onLoaded: (LoadedImage) -> Unit,
) {
    val result by produceState<Result<LoadedImage>?>(null, item.path) {
        val image = viewModel.load(item)
        // The dialog only needs the details, which do not keep the bitmap alive once the page is gone.
        if (image != null) onLoaded(image.copy(bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)))
        value = if (image != null) Result.success(image) else Result.failure(IllegalStateException())
    }
    val current = result
    if (current == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val image = current.getOrNull()
    if (image == null) {
        ViewerMessage(R.string.viewer_error, Modifier.fillMaxSize())
        return
    }
    val rotation = viewModel.rotations[item.path] ?: 0
    val bitmap = remember(image, rotation) {
        if (rotation == 0) {
            image.bitmap
        } else {
            val matrix = Matrix().apply { setRotate(rotation.toFloat()) }
            Bitmap.createBitmap(image.bitmap, 0, 0, image.bitmap.width, image.bitmap.height, matrix, true)
        }
    }
    ZoomableImage(bitmap, item.name, settled)
}

/** Pinch and double-tap zoom. Panning stops at the image's edges; dragging further turns the page. */
@Composable
private fun ZoomableImage(bitmap: Bitmap, description: String, settled: Boolean) {
    var scale by remember(bitmap) { mutableFloatStateOf(1f) }
    var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }
    LaunchedEffect(settled) {
        if (!settled) {
            scale = 1f
            offset = Offset.Zero
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val fit = minOf(width / bitmap.width, height / bitmap.height)
        val centre = Offset(width / 2, height / 2)

        fun bounded(target: Offset, at: Float) = Offset(
            Zoom.clampPan(target.x, bitmap.width * fit * at, width),
            Zoom.clampPan(target.y, bitmap.height * fit * at, height),
        )

        fun zoomTo(target: Float, focus: Offset) {
            val next = target.coerceIn(1f, MAX_IMAGE_ZOOM)
            val fromCentre = focus - centre
            offset = bounded((offset - fromCentre) * (next / scale) + fromCentre, next)
            scale = next
        }

        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = description,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(bitmap, width, height) {
                    detectTapGestures(onDoubleTap = { zoomTo(if (scale > 1f) 1f else DOUBLE_TAP_IMAGE_ZOOM, it) })
                }
                .pointerInput(bitmap, width, height) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val pinching = event.changes.count { it.pressed } >= 2
                            if (pinching) zoomTo(scale * event.calculateZoom(), event.calculateCentroid())
                            if (pinching || scale > 1f) {
                                val pan = event.calculatePan()
                                val before = offset
                                offset = bounded(offset + pan, scale)
                                // Not consumed at a side edge, so the pager can take the swipe.
                                val atEdge = kotlin.math.abs(pan.x) > kotlin.math.abs(pan.y) && offset.x == before.x
                                if (pinching || (pan != Offset.Zero && !atEdge)) event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
    }
}

@Composable
private fun ImageInfo(item: FileItem, width: Int?, height: Int?, taken: String?, camera: String?, onClose: () -> Unit) {
    val context = LocalContext.current
    val rows = listOfNotNull(
        R.string.image_info_name to item.name,
        if (width != null && height != null) {
            R.string.image_info_dimensions to stringResource(R.string.image_dimensions_value, width, height)
        } else {
            null
        },
        R.string.image_info_size to Formatter.formatFileSize(context, item.sizeBytes),
        R.string.image_info_modified to DateFormat.getDateTimeInstance().format(Date(item.lastModifiedMillis)),
        taken?.let { R.string.image_info_taken to it },
        camera?.let { R.string.image_info_camera to it },
        R.string.image_info_location to item.path,
    )
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.image_info)) },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rows.forEach { (label, value) ->
                    Column {
                        Text(stringResource(label), style = MaterialTheme.typography.labelMedium)
                        Text(value)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.viewer_close)) } },
    )
}

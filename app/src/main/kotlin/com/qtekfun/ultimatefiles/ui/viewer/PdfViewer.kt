package com.qtekfun.ultimatefiles.ui.viewer

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qtekfun.ultimatefiles.R
import com.qtekfun.ultimatefiles.domain.repository.ReadingPosition
import com.qtekfun.ultimatefiles.domain.usecase.Zoom
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MAX_PDF_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f
private const val RENDER_DELAY_MILLIS = 250L
private const val A4_RATIO = 0.707f

@Composable
internal fun PdfViewer(viewModel: PdfViewModel, modifier: Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        PdfState.Loading -> Box(modifier, Alignment.Center) { CircularProgressIndicator() }
        PdfState.Failed -> ViewerMessage(R.string.viewer_error, modifier)
        is PdfState.Ready -> PdfPages(viewModel, current, modifier)
    }
}

/** All the pages in a column. Zooming widens the pages; they are drawn again once the pinch settles. */
@Composable
private fun PdfPages(viewModel: PdfViewModel, document: PdfState.Ready, modifier: Modifier) {
    val list = rememberLazyListState(document.position.page)
    val scope = rememberCoroutineScope()
    var scale by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var renderScale by remember { mutableFloatStateOf(1f) }
    var askPage by rememberSaveable { mutableStateOf(false) }
    val ratios = remember { mutableStateMapOf<Int, Float>() }
    val page by remember { derivedStateOf { list.firstVisibleItemIndex } }
    // The page filling the middle of the screen is the one being read.
    val shownPage by remember {
        derivedStateOf {
            val info = list.layoutInfo
            val middle = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.firstOrNull { it.offset + it.size > middle }?.index ?: page
        }
    }

    fun position(): ReadingPosition {
        val first = list.layoutInfo.visibleItemsInfo.firstOrNull()
        val fraction = if (first == null || first.size == 0) 0f else list.firstVisibleItemScrollOffset.toFloat() / first.size
        return ReadingPosition(list.firstVisibleItemIndex, fraction)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.remember(position()) }
    DisposableEffect(viewModel) { onDispose { viewModel.remember(position()) } }
    LaunchedEffect(Unit) {
        val saved = document.position
        // The item's height is only known once it is on screen.
        delay(RENDER_DELAY_MILLIS)
        val item = list.layoutInfo.visibleItemsInfo.firstOrNull { it.index == saved.page }
        if (item != null && list.firstVisibleItemIndex == saved.page) {
            list.scrollToItem(saved.page, (item.size * saved.fraction).toInt())
        }
    }
    LaunchedEffect(scale) {
        delay(RENDER_DELAY_MILLIS)
        renderScale = scale
    }

    Column(modifier) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            val width = constraints.maxWidth

            fun zoomTo(target: Float, focusX: Float, focusY: Float) {
                val next = target.coerceIn(1f, MAX_PDF_ZOOM)
                val factor = next / scale
                if (factor == 1f) return
                panX = Zoom.clampScroll(Zoom.anchored(panX, focusX, factor), width * next, width.toFloat())
                val offset = Zoom.anchored(list.firstVisibleItemScrollOffset.toFloat(), focusY, factor)
                list.requestScrollToItem(list.firstVisibleItemIndex, offset.toInt())
                scale = next
            }

            LazyColumn(
                state = list,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(width) {
                        detectTapGestures(onDoubleTap = { zoomTo(if (scale > 1f) 1f else DOUBLE_TAP_ZOOM, it.x, it.y) })
                    }
                    .pointerInput(width) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val fingers = event.changes.count { it.pressed }
                                if (fingers >= 2) {
                                    val centre = event.calculateCentroid()
                                    zoomTo(scale * event.calculateZoom(), centre.x, centre.y)
                                    val pan = event.calculatePan()
                                    panX = Zoom.clampScroll(panX - pan.x, width * scale, width.toFloat())
                                    list.dispatchRawDelta(-pan.y)
                                    event.changes.forEach { it.consume() }
                                } else if (fingers == 1) {
                                    // Vertical movement is left to the list; only the sideways part pans here.
                                    val dx = event.changes.first { it.pressed }.positionChange().x
                                    panX = Zoom.clampScroll(panX - dx, width * scale, width.toFloat())
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    },
            ) {
                items(document.pageCount, key = { it }) { index ->
                    PdfPage(
                        viewModel = viewModel,
                        index = index,
                        targetWidth = (width * renderScale).toInt(),
                        ratio = ratios[index] ?: ratios[0] ?: A4_RATIO,
                        onRatio = { ratios[index] = it },
                        modifier = Modifier.layout { measurable, outer ->
                            val wide = (outer.maxWidth * scale).toInt()
                            val placeable = measurable.measure(Constraints.fixedWidth(wide))
                            layout(outer.maxWidth, placeable.height) { placeable.place(-panX.toInt(), 0) }
                        },
                    )
                }
            }
        }
        Surface(tonalElevation = 3.dp) {
            TextButton(onClick = { askPage = true }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(stringResource(R.string.pdf_page_of, shownPage + 1, document.pageCount))
            }
        }
    }

    if (askPage) {
        GoToPageDialog(document.pageCount, onDismiss = { askPage = false }) { target ->
            askPage = false
            scope.launch { list.scrollToItem(target) }
        }
    }
}

@Composable
private fun PdfPage(
    viewModel: PdfViewModel,
    index: Int,
    targetWidth: Int,
    ratio: Float,
    onRatio: (Float) -> Unit,
    modifier: Modifier,
) {
    val image by produceState(viewModel.cached(index), index, targetWidth) {
        value = viewModel.render(index, targetWidth) ?: value
    }
    val bitmap = image?.bitmap
    if (bitmap != null) LaunchedEffect(bitmap) { onRatio(bitmap.width.toFloat() / bitmap.height) }
    Box(modifier.aspectRatio(if (bitmap != null) bitmap.width.toFloat() / bitmap.height else ratio), Alignment.Center) {
        if (bitmap == null) {
            CircularProgressIndicator()
        } else {
            Image(
                bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.pdf_page_description, index + 1),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun GoToPageDialog(count: Int, onDismiss: () -> Unit, onGo: (Int) -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    val target = Zoom.pageIndex(input, count)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pdf_go_to_page)) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(stringResource(R.string.pdf_page_range, count)) },
                isError = input.isNotEmpty() && target == null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { target?.let(onGo) }),
            )
        },
        confirmButton = {
            TextButton(onClick = { target?.let(onGo) }, enabled = target != null) { Text(stringResource(R.string.pdf_go)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.editor_cancel)) } },
    )
}

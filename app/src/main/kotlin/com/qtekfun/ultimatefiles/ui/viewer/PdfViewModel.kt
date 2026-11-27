package com.qtekfun.ultimatefiles.ui.viewer

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtekfun.ultimatefiles.domain.repository.ReadingPosition
import com.qtekfun.ultimatefiles.domain.repository.ReadingPositionStore
import com.qtekfun.ultimatefiles.domain.usecase.Zoom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface PdfState {
    data object Loading : PdfState
    data class Ready(val pageCount: Int, val position: ReadingPosition) : PdfState
    data object Failed : PdfState
}

class PdfPageImage(val bitmap: Bitmap, val targetWidth: Int)

/** Largest bitmap for one page (16 MB), and for all the cached ones together. */
private const val MAX_PAGE_PIXELS = 4 * 1024 * 1024
private const val MAX_CACHE_BYTES = 48 * 1024 * 1024

class PdfViewModel(
    private val open: () -> ParcelFileDescriptor?,
    private val path: String,
    private val positions: ReadingPositionStore,
) : ViewModel() {
    private val _state = MutableStateFlow<PdfState>(PdfState.Loading)
    val state = _state.asStateFlow()

    // PdfRenderer allows a single open page and is not thread safe.
    private val lock = Mutex()
    private var renderer: PdfRenderer? = null
    private val cache = object : LruCache<Int, PdfPageImage>(
        minOf(Runtime.getRuntime().maxMemory() / 8, MAX_CACHE_BYTES.toLong()).toInt(),
    ) {
        override fun sizeOf(key: Int, value: PdfPageImage) = value.bitmap.allocationByteCount
    }

    init {
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) {
                runCatching {
                    val descriptor = open() ?: error("Cannot open the file")
                    val opened = runCatching { PdfRenderer(descriptor) }.onFailure { descriptor.close() }.getOrThrow()
                    lock.withLock { renderer = opened }
                    opened.pageCount
                }.getOrNull()
            }
            _state.value = if (count == null || count == 0) {
                PdfState.Failed
            } else {
                val saved = positions.load(path)?.takeIf { it.page < count } ?: ReadingPosition(0, 0f)
                PdfState.Ready(count, saved)
            }
        }
    }

    fun cached(index: Int): PdfPageImage? = cache.get(index)

    /** The page [index] about [targetWidth] pixels wide (less if that would be too big), or null if it cannot be drawn. */
    suspend fun render(index: Int, targetWidth: Int): PdfPageImage? {
        cache.get(index)?.let { if (it.targetWidth == targetWidth) return it }
        return withContext(Dispatchers.IO) {
            lock.withLock {
                runCatching {
                    renderer?.openPage(index)?.use { page ->
                        val (width, height) = Zoom.renderSize(page.width, page.height, targetWidth, MAX_PAGE_PIXELS)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        PdfPageImage(bitmap, targetWidth).also { cache.put(index, it) }
                    }
                }.getOrNull()
            }
        }
    }

    fun remember(position: ReadingPosition) {
        viewModelScope.launch(NonCancellable) { positions.save(path, position) }
    }

    override fun onCleared() {
        cache.evictAll()
        CoroutineScope(Dispatchers.IO).launch {
            lock.withLock {
                renderer?.close()
                renderer = null
            }
        }
    }
}

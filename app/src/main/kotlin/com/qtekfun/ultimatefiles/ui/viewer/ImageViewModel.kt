package com.qtekfun.ultimatefiles.ui.viewer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtekfun.ultimatefiles.core.model.FileItem
import com.qtekfun.ultimatefiles.domain.repository.FileSystemRepository
import com.qtekfun.ultimatefiles.domain.usecase.ImageSiblings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface GalleryState {
    data object Loading : GalleryState
    data class Ready(val images: List<FileItem>, val start: Int) : GalleryState
    data object Failed : GalleryState
}

/** A decoded image, already upright, with the original size and what the camera recorded. */
data class LoadedImage(val bitmap: Bitmap, val width: Int, val height: Int, val taken: String?, val camera: String?)

private const val MAX_IMAGE_SIDE = 2048

class ImageViewModel(private val repository: FileSystemRepository, private val path: String) : ViewModel() {
    private val _state = MutableStateFlow<GalleryState>(GalleryState.Loading)
    val state = _state.asStateFlow()

    /** Degrees each image was turned on screen; the file is not changed. */
    val rotations = mutableStateMapOf<String, Int>()

    init {
        viewModelScope.launch {
            val current = repository.stat(path).getOrNull()
            _state.value = if (current == null) {
                GalleryState.Failed
            } else {
                // Without a listable parent folder the image is shown alone.
                val folder = repository.parentOf(path)?.let { repository.listFiles(it).getOrNull() }.orEmpty()
                val (images, start) = ImageSiblings.of(current, folder)
                GalleryState.Ready(images, start)
            }
        }
    }

    fun rotate(path: String) {
        rotations[path] = ((rotations[path] ?: 0) + 90) % 360
    }

    suspend fun load(item: FileItem): LoadedImage? = withContext(Dispatchers.IO) {
        try {
            suspend fun input() = repository.openInput(item).getOrThrow()
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            input().use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_IMAGE_SIDE) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = input().use { BitmapFactory.decodeStream(it, null, options) } ?: return@withContext null
            val exif = runCatching { input().use { ExifInterface(it) } }.getOrNull()
            val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val matrix = orientationMatrix(orientation)
            val upright = if (matrix.isIdentity) {
                decoded
            } else {
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            }
            val sideways = orientation != null && orientation >= ExifInterface.ORIENTATION_TRANSPOSE
            LoadedImage(
                bitmap = upright,
                width = if (sideways) bounds.outHeight else bounds.outWidth,
                height = if (sideways) bounds.outWidth else bounds.outHeight,
                taken = exif?.getAttribute(ExifInterface.TAG_DATETIME),
                camera = exif?.getAttribute(ExifInterface.TAG_MODEL),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    private fun orientationMatrix(orientation: Int?) = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                setRotate(90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                setRotate(270f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
        }
    }
}

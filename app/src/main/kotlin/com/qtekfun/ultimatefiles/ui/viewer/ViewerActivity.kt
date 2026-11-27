package com.qtekfun.ultimatefiles.ui.viewer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qtekfun.ultimatefiles.R
import com.qtekfun.ultimatefiles.core.model.AccentColor
import com.qtekfun.ultimatefiles.core.model.ThemeMode
import com.qtekfun.ultimatefiles.domain.usecase.ViewerKind
import com.qtekfun.ultimatefiles.ui.settings.SettingsViewModel
import com.qtekfun.ultimatefiles.ui.theme.UltimateFilesTheme
import org.koin.mp.KoinPlatform

/**
 * Shows images, text, PDFs and audio/video without leaving the app. It only receives a `content://` URI and reads it
 * through the content resolver, so it works for local files and for storage-access-framework documents alike.
 */
class ViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val uri: Uri? = intent.data
        val name = intent.getStringExtra(EXTRA_NAME).orEmpty()
        val path = intent.getStringExtra(EXTRA_PATH)
        val declared = intent.getStringExtra(EXTRA_KIND)?.let { runCatching { ViewerKind.valueOf(it) }.getOrNull() }
        setContent {
            // Saved, so the choice made in "open as" survives the activity being recreated.
            var kind by rememberSaveable { mutableStateOf(declared) }
            val koin = remember { KoinPlatform.getKoin() }
            val settings = viewModel<SettingsViewModel>(
                factory = viewModelFactory { initializer { koin.get<SettingsViewModel>() } },
            )
            val preferences by settings.preferences.collectAsStateWithLifecycle()
            UltimateFilesTheme(
                themeMode = preferences?.themeMode ?: ThemeMode.SYSTEM,
                dynamicColor = preferences?.dynamicColor ?: false,
                accent = preferences?.accentColor ?: AccentColor.CYAN,
            ) {
                if (kind == null && uri != null) {
                    OpenAsScreen(name = name, onKind = { kind = it }, onOtherApp = { openElsewhere(uri) }, onBack = { finish() })
                } else if (kind == ViewerKind.TEXT && path != null) {
                    val editor = viewModel<TextEditorViewModel>(
                        factory = viewModelFactory { initializer { TextEditorViewModel(koin.get(), path, koin.get()) } },
                    )
                    TextEditorScreen(name = name, viewModel = editor, onClose = { finish() })
                } else if (kind == ViewerKind.IMAGE && path != null) {
                    val gallery = viewModel<ImageViewModel>(
                        factory = viewModelFactory { initializer { ImageViewModel(koin.get(), path) } },
                    )
                    ImageGalleryScreen(name = name, viewModel = gallery, onBack = { finish() })
                } else {
                    val pdf = if (kind == ViewerKind.PDF && uri != null) {
                        viewModel<PdfViewModel>(
                            factory = viewModelFactory {
                                initializer {
                                    val resolver = applicationContext.contentResolver
                                    PdfViewModel({ resolver.openFileDescriptor(uri, "r") }, path ?: uri.toString(), koin.get())
                                }
                            },
                        )
                    } else {
                        null
                    }
                    val media = if (kind == ViewerKind.MEDIA && uri != null) {
                        viewModel<MediaViewModel>(
                            factory = viewModelFactory { initializer { MediaViewModel(application, uri) } },
                        )
                    } else {
                        null
                    }
                    ViewerScreen(name = name, pdf = pdf, media = media, onBack = { finish() })
                }
            }
        }
    }

    private fun openElsewhere(uri: Uri) {
        val view = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // The chooser itself reports that no application can open the file.
        startActivity(Intent.createChooser(view, null))
        finish()
    }

    companion object {
        const val EXTRA_NAME = "viewer_name"
        const val EXTRA_KIND = "viewer_kind"

        /** The file's path as the repository knows it. */
        const val EXTRA_PATH = "viewer_path"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
private fun ViewerScreen(name: String, pdf: PdfViewModel?, media: MediaViewModel?, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(name, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        val content = Modifier.padding(padding).fillMaxSize()
        when {
            pdf != null -> PdfViewer(pdf, content)
            media != null -> MediaViewer(media, content)
            else -> Text(stringResource(R.string.viewer_unsupported), modifier = content)
        }
    }
}

@androidx.compose.runtime.Composable
internal fun ViewerMessage(resId: Int, modifier: Modifier, detail: String? = null) {
    Text(
        text = listOfNotNull(stringResource(resId), detail).joinToString(": "),
        color = MaterialTheme.colorScheme.error,
        modifier = modifier,
    )
}

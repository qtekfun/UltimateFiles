package com.qtekfun.ultimatefiles.ui.viewer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatefiles.R
import com.qtekfun.ultimatefiles.domain.usecase.ViewerKind

/** For a file of unknown type: the user says how to open it. */
@Composable
internal fun OpenAsScreen(name: String, onKind: (ViewerKind) -> Unit, onOtherApp: () -> Unit, onBack: () -> Unit) {
    Surface {
        Column(
            Modifier.safeDrawingPadding().fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.open_as_title, name), style = MaterialTheme.typography.titleLarge)
            listOf(
                ViewerKind.TEXT to R.string.open_as_text,
                ViewerKind.IMAGE to R.string.open_as_image,
                ViewerKind.PDF to R.string.open_as_pdf,
                ViewerKind.MEDIA to R.string.open_as_media,
            ).forEach { (kind, label) ->
                OutlinedButton(onClick = { onKind(kind) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(label)) }
            }
            OutlinedButton(onClick = onOtherApp, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.open_as_other_app))
            }
            TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
        }
    }
}

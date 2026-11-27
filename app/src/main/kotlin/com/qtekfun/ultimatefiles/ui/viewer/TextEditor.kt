package com.qtekfun.ultimatefiles.ui.viewer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qtekfun.ultimatefiles.R
import com.qtekfun.ultimatefiles.domain.usecase.SyntaxHighlighter
import com.qtekfun.ultimatefiles.domain.usecase.TextReadOnly
import com.qtekfun.ultimatefiles.domain.usecase.TextSearch
import com.qtekfun.ultimatefiles.domain.usecase.TokenKind

/** Find bar contents; [current] indexes the matches of [query]. */
private data class FindState(val query: String = "", val replacement: String = "", val current: Int = 0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TextEditorScreen(name: String, viewModel: TextEditorViewModel, onClose: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val field = viewModel.field
    val editable = !state.loading && state.readOnly == null
    var find by remember { mutableStateOf<FindState?>(null) }
    var menu by rememberSaveable { mutableStateOf(false) }
    val markdown = name.substringAfterLast('.', "").lowercase() in setOf("md", "markdown")
    var preview by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val matches = remember(find?.query, field.text.toString()) { TextSearch.matches(field.text, find?.query.orEmpty()) }
    val currentMatch = find?.let { if (matches.isEmpty()) -1 else it.current.mod(matches.size) } ?: -1

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onEvent(EditorEvent.Persist) }
    BackHandler {
        if (find != null) find = null else viewModel.onEvent(EditorEvent.Exit)
    }
    LaunchedEffect(state.closed) { if (state.closed) onClose() }
    LaunchedEffect(field) { snapshotFlow { field.text.toString() }.collect { viewModel.onEvent(EditorEvent.TextChanged) } }

    val errorText = when (state.error) {
        EditorError.LOAD -> stringResource(R.string.viewer_error)
        EditorError.SAVE -> stringResource(R.string.editor_save_failed)
        EditorError.ENCODING -> stringResource(R.string.editor_encoding_failed)
        null -> null
    }
    LaunchedEffect(errorText) {
        if (errorText != null) {
            snackbar.showSnackbar(errorText)
            viewModel.onEvent(EditorEvent.ErrorShown)
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(if (state.dirty) stringResource(R.string.editor_modified, name) else name, maxLines = 1) },
                navigationIcon = {
                    Action(Icons.AutoMirrored.Filled.ArrowBack, R.string.action_back) { viewModel.onEvent(EditorEvent.Exit) }
                },
                actions = {
                    if (editable) {
                        Action(Icons.AutoMirrored.Filled.Undo, R.string.editor_undo, state.canUndo) {
                            viewModel.onEvent(EditorEvent.Undo)
                        }
                        Action(Icons.AutoMirrored.Filled.Redo, R.string.editor_redo, state.canRedo) {
                            viewModel.onEvent(EditorEvent.Redo)
                        }
                        Action(Icons.Filled.Save, R.string.editor_save, state.dirty && !state.saving) {
                            viewModel.onEvent(EditorEvent.Save)
                        }
                    }
                    if (markdown) {
                        Action(
                            if (preview) Icons.Filled.Edit else Icons.Filled.Visibility,
                            if (preview) R.string.editor_show_source else R.string.editor_show_preview,
                            !state.loading,
                        ) {
                            preview = !preview
                            find = null
                        }
                    }
                    Action(Icons.Filled.Search, R.string.editor_find, !state.loading && !preview) {
                        find = if (find == null) FindState() else null
                    }
                    Box {
                        Action(Icons.Filled.MoreVert, R.string.editor_more) { menu = true }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            ToggleItem(R.string.editor_word_wrap, state.wordWrap) {
                                menu = false
                                viewModel.onEvent(EditorEvent.ToggleWordWrap)
                            }
                            ToggleItem(R.string.editor_line_numbers, state.lineNumbers) {
                                menu = false
                                viewModel.onEvent(EditorEvent.ToggleLineNumbers)
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.saving) LinearProgressIndicator(Modifier.fillMaxWidth())
            val notice = when {
                state.readOnly == TextReadOnly.TOO_LARGE -> R.string.editor_read_only_large
                state.readOnly == TextReadOnly.BINARY -> R.string.editor_read_only_binary
                state.readOnly == TextReadOnly.NOT_WRITABLE -> R.string.editor_read_only_permission
                state.recovered -> R.string.editor_recovered
                state.restoredDraft -> R.string.editor_restored_draft
                else -> null
            }
            if (notice != null) {
                Text(
                    stringResource(notice),
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            find?.let { current ->
                FindBar(
                    find = current,
                    position = currentMatch + 1,
                    count = matches.size,
                    canReplace = editable,
                    onChange = { find = it },
                    onReplace = {
                        if (currentMatch >= 0) {
                            viewModel.onEvent(
                                EditorEvent.ReplaceMatch(matches[currentMatch], current.query.length, current.replacement),
                            )
                            // The replaced match is gone, unless the replacement contains the query again.
                            val skip = TextSearch.matches(current.replacement, current.query).size
                            find = current.copy(current = currentMatch + skip)
                        }
                    },
                    onReplaceAll = { viewModel.onEvent(EditorEvent.ReplaceAll(current.query, current.replacement)) },
                    onClose = { find = null },
                )
            }
            if (state.loading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            } else if (preview) {
                MarkdownPreview(field.text.toString(), Modifier.fillMaxSize())
            } else {
                EditorBody(
                    name = name,
                    viewModel = viewModel,
                    state = state,
                    match = if (currentMatch >= 0) matches[currentMatch] else -1,
                    matchLength = find?.query?.length ?: 0,
                )
            }
        }
    }

    if (state.confirmExit) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(EditorEvent.CancelExit) },
            title = { Text(stringResource(R.string.editor_unsaved_title)) },
            text = { Text(stringResource(R.string.editor_unsaved_message, name)) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(EditorEvent.SaveAndExit) }) { Text(stringResource(R.string.editor_save)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { viewModel.onEvent(EditorEvent.CancelExit) }) {
                        Text(stringResource(R.string.editor_cancel))
                    }
                    TextButton(onClick = { viewModel.onEvent(EditorEvent.DiscardAndExit) }) {
                        Text(stringResource(R.string.editor_discard))
                    }
                }
            },
        )
    }
}

@Composable
private fun Action(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled) { Icon(icon, contentDescription = stringResource(label)) }
}

@Composable
private fun ToggleItem(label: Int, checked: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(label)) },
        trailingIcon = { Checkbox(checked = checked, onCheckedChange = null) },
        onClick = onClick,
    )
}

@Composable
private fun FindBar(
    find: FindState,
    position: Int,
    count: Int,
    canReplace: Boolean,
    onChange: (FindState) -> Unit,
    onReplace: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = find.query,
                onValueChange = { onChange(find.copy(query = it, current = 0)) },
                label = { Text(stringResource(R.string.editor_find)) },
                suffix = { if (find.query.isNotEmpty()) Text(stringResource(R.string.editor_match_count, position, count)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Action(Icons.Filled.KeyboardArrowUp, R.string.editor_previous, count > 0) {
                onChange(find.copy(current = (position - 2).mod(count)))
            }
            Action(Icons.Filled.KeyboardArrowDown, R.string.editor_next, count > 0) {
                onChange(find.copy(current = position.mod(count)))
            }
            Action(Icons.Filled.Close, R.string.editor_close_find, onClick = onClose)
        }
        if (canReplace) {
            OutlinedTextField(
                value = find.replacement,
                onValueChange = { onChange(find.copy(replacement = it)) },
                label = { Text(stringResource(R.string.editor_replace_with)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row {
                TextButton(onClick = onReplace, enabled = count > 0) { Text(stringResource(R.string.editor_replace)) }
                TextButton(onClick = onReplaceAll, enabled = count > 0) { Text(stringResource(R.string.editor_replace_all)) }
            }
        }
    }
}

/** The text with its line-number gutter. Without word wrap the field is as wide as its longest line and pans sideways. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EditorBody(name: String, viewModel: TextEditorViewModel, state: EditorState, match: Int, matchLength: Int) {
    val field = viewModel.field
    val vertical = rememberScrollState()
    val horizontal = rememberScrollState()
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var layoutVersion by remember { mutableIntStateOf(0) }
    var viewportHeight by remember { mutableIntStateOf(0) }
    val style = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
        color = MaterialTheme.colorScheme.onSurface,
    )
    val numberStyle = style.copy(color = MaterialTheme.colorScheme.outline)
    val highlight = MaterialTheme.colorScheme.tertiaryContainer
    val measurer = rememberTextMeasurer()
    val colors = MaterialTheme.colorScheme
    val highlighting = remember(name, colors) {
        OutputTransformation {
            SyntaxHighlighter.tokens(name, asCharSequence()).forEach {
                val color = when (it.kind) {
                    TokenKind.COMMENT -> colors.outline
                    TokenKind.STRING -> colors.tertiary
                    TokenKind.NUMBER -> colors.secondary
                    TokenKind.KEYWORD, TokenKind.TAG -> colors.primary
                }
                addStyle(SpanStyle(color = color), it.start, it.end)
            }
        }
    }
    val inset = 8.dp
    val insetPx = with(LocalDensity.current) { inset.toPx() }

    // Brings the current match into view.
    LaunchedEffect(match, layoutVersion == 0) {
        val result = layout ?: return@LaunchedEffect
        if (match < 0 || match >= result.layoutInput.text.length) return@LaunchedEffect
        val box = result.getBoundingBox(match)
        vertical.animateScrollTo((box.top - viewportHeight / 3).toInt().coerceAtLeast(0))
        if (!state.wordWrap) horizontal.animateScrollTo((box.left - insetPx * 4).toInt().coerceAtLeast(0))
    }

    Row(Modifier.fillMaxSize()) {
        if (state.lineNumbers) {
            val starts = remember(layoutVersion) { TextSearch.lineStarts(layout?.layoutInput?.text ?: "") }
            val widest = remember(starts.size, numberStyle) { measurer.measure(starts.size.toString(), numberStyle).size.width }
            val width = with(LocalDensity.current) { widest.toDp() } + inset * 2
            Canvas(Modifier.width(width).fillMaxHeight().clipToBounds()) {
                val result = layout ?: return@Canvas
                val length = result.layoutInput.text.length
                val topVisual = result.getLineForVerticalPosition((vertical.value - insetPx).coerceAtLeast(0f))
                val found = starts.binarySearch(result.getLineStart(topVisual))
                var line = if (found >= 0) found else (-found - 2).coerceAtLeast(0)
                while (line < starts.size && starts[line] <= length) {
                    val top = result.getLineTop(result.getLineForOffset(starts[line])) - vertical.value + insetPx
                    if (top > size.height) break
                    val number = measurer.measure((line + 1).toString(), numberStyle)
                    drawText(number, topLeft = Offset(size.width - insetPx - number.size.width, top))
                    line++
                }
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
            val available = maxWidth
            viewportHeight = constraints.maxHeight
            Box(if (state.wordWrap) Modifier.fillMaxSize() else Modifier.fillMaxSize().horizontalScroll(horizontal)) {
                BasicTextField(
                    state = field,
                    readOnly = state.readOnly != null || state.saving,
                    textStyle = style,
                    outputTransformation = highlighting,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    lineLimits = TextFieldLineLimits.MultiLine(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Text,
                    ),
                    scrollState = vertical,
                    onTextLayout = { result ->
                        layout = result()
                        layoutVersion++
                    },
                    modifier = (if (state.wordWrap) Modifier.fillMaxWidth() else Modifier.widthIn(min = available))
                        .fillMaxHeight()
                        .padding(horizontal = inset)
                        .drawBehind {
                            val result = layout ?: return@drawBehind
                            if (match < 0 || match + matchLength > result.layoutInput.text.length) return@drawBehind
                            clipRect {
                                translate(top = insetPx - vertical.value) {
                                    drawPath(result.getPathForRange(match, match + matchLength), highlight)
                                }
                            }
                        }
                        .padding(top = inset),
                )
            }
        }
    }
}

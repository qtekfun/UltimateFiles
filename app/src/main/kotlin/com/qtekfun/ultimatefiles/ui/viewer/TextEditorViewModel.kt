package com.qtekfun.ultimatefiles.ui.viewer

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtekfun.ultimatefiles.domain.repository.DraftStore
import com.qtekfun.ultimatefiles.domain.repository.FileSystemRepository
import com.qtekfun.ultimatefiles.domain.usecase.EditHistory
import com.qtekfun.ultimatefiles.domain.usecase.TextDocument
import com.qtekfun.ultimatefiles.domain.usecase.TextReadOnly
import com.qtekfun.ultimatefiles.domain.usecase.TextSearch
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream

enum class EditorError { LOAD, SAVE, ENCODING }

data class EditorState(
    val loading: Boolean = true,
    val readOnly: TextReadOnly? = null,
    val dirty: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val saving: Boolean = false,
    val recovered: Boolean = false,
    /** Unsaved text from an editor that was destroyed is on screen. */
    val restoredDraft: Boolean = false,
    val wordWrap: Boolean = true,
    val lineNumbers: Boolean = true,
    val confirmExit: Boolean = false,
    val error: EditorError? = null,
    /** The user is done: the screen must close. */
    val closed: Boolean = false,
)

sealed interface EditorEvent {
    /** The text field changed, by typing or by a replace. */
    data object TextChanged : EditorEvent
    data object Undo : EditorEvent
    data object Redo : EditorEvent
    data object Save : EditorEvent
    data class ReplaceMatch(val start: Int, val length: Int, val replacement: String) : EditorEvent
    data class ReplaceAll(val query: String, val replacement: String) : EditorEvent
    data object ToggleWordWrap : EditorEvent
    data object ToggleLineNumbers : EditorEvent
    data object Exit : EditorEvent
    data object SaveAndExit : EditorEvent
    data object DiscardAndExit : EditorEvent
    data object CancelExit : EditorEvent
    data object ErrorShown : EditorEvent
    /** The screen is no longer in front and may be destroyed without further notice. */
    data object Persist : EditorEvent
}

private const val DRAFT_DELAY_MILLIS = 1500L

class TextEditorViewModel(
    private val repository: FileSystemRepository,
    private val path: String,
    private val drafts: DraftStore,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    val field = TextFieldState()

    private val _state = MutableStateFlow(EditorState())
    val state = _state.asStateFlow()

    private var document: TextDocument? = null
    private var savedText = ""
    private var history = EditHistory("")
    private val draftLock = Mutex()
    private var draftJob: Job? = null

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        try {
            val item = repository.stat(path).getOrThrow()
            val input = repository.openInput(item).getOrThrow()
            val onDisk = withContext(dispatcher) { TextDocument.decode(input.use(::readLimited), item.isWritable) }
            val backup = repository.interruptedSaveBackup(item)
                ?.let { withContext(dispatcher) { TextDocument.decode(it, item.isWritable) } }
            val recovered = backup != null && backup.readOnly == null && backup.text != onDisk.text
            val shown = if (recovered) backup!! else onDisk
            document = shown
            savedText = onDisk.text
            history = EditHistory(shown.text)
            val draft = if (shown.readOnly == null) drafts.load(path)?.takeIf { it != shown.text } else null
            // Recorded as an edit, so undo goes back to what is on disk.
            if (draft != null) history.record(draft)
            field.setTextAndPlaceCursorAtEnd(draft ?: shown.text)
            field.edit { selection = TextRange.Zero }
            _state.update {
                it.copy(
                    loading = false,
                    readOnly = shown.readOnly,
                    recovered = recovered,
                    restoredDraft = draft != null,
                    dirty = recovered || draft != null,
                    canUndo = history.canUndo,
                )
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, error = EditorError.LOAD) }
        }
    }

    /** Reads at most one byte more than can be edited, which is enough to know the file is too large. */
    private fun readLimited(input: InputStream): ByteArray {
        val buffer = ByteArray(TextDocument.MAX_EDIT_BYTES + 1)
        var read = 0
        while (read < buffer.size) {
            val n = input.read(buffer, read, buffer.size - read)
            if (n < 0) break
            read += n
        }
        return buffer.copyOf(read)
    }

    fun onEvent(event: EditorEvent) {
        when (event) {
            EditorEvent.TextChanged -> {
                record()
                persistSoon()
            }
            EditorEvent.Undo -> {
                record()
                history.undo()?.let(::apply)
            }
            EditorEvent.Redo -> {
                record()
                history.redo()?.let(::apply)
            }
            EditorEvent.Save -> save(thenClose = false)
            is EditorEvent.ReplaceMatch -> if (editable() && event.start + event.length <= field.text.length) {
                field.edit {
                    replace(event.start, event.start + event.length, event.replacement)
                    selection = TextRange(event.start + event.replacement.length)
                }
                record()
            }
            is EditorEvent.ReplaceAll -> if (editable()) {
                val replaced = TextSearch.replaceAll(field.text, event.query, event.replacement)
                field.edit {
                    replace(0, length, replaced)
                    selection = TextRange.Zero
                }
                record()
            }
            EditorEvent.ToggleWordWrap -> _state.update { it.copy(wordWrap = !it.wordWrap) }
            EditorEvent.ToggleLineNumbers -> _state.update { it.copy(lineNumbers = !it.lineNumbers) }
            EditorEvent.Exit -> _state.update { if (it.dirty) it.copy(confirmExit = true) else it.copy(closed = true) }
            EditorEvent.SaveAndExit -> {
                _state.update { it.copy(confirmExit = false) }
                save(thenClose = true)
            }
            EditorEvent.DiscardAndExit -> {
                _state.update { it.copy(confirmExit = false, closed = true) }
                persist()
            }
            EditorEvent.Persist -> persist()
            EditorEvent.CancelExit -> _state.update { it.copy(confirmExit = false) }
            EditorEvent.ErrorShown -> _state.update { it.copy(error = null) }
        }
    }

    /** Typing is also kept while the editor stays in front, in case the process dies there. */
    private fun persistSoon() {
        draftJob?.cancel()
        if (!_state.value.dirty) return
        draftJob = viewModelScope.launch {
            delay(DRAFT_DELAY_MILLIS)
            persist()
        }
    }

    /** Writes the draft while there are unsaved changes and removes it once they are saved or discarded. */
    private fun persist() {
        if (!editable()) return
        // Not cancellable: it must finish even if the editor is being destroyed.
        viewModelScope.launch(NonCancellable) {
            draftLock.withLock {
                val current = _state.value
                if (current.dirty && !current.closed) drafts.save(path, field.text.toString()) else drafts.delete(path)
            }
        }
    }

    private fun editable() = !_state.value.loading && document?.readOnly == null

    private fun apply(edit: EditHistory.Edit) {
        field.edit {
            replace(edit.start, edit.start + edit.length, edit.text)
            selection = TextRange(edit.start + edit.text.length)
        }
        refresh()
    }

    private fun record() {
        if (!editable()) return
        history.record(field.text.toString())
        refresh()
    }

    private fun refresh() = _state.update {
        it.copy(
            dirty = it.recovered || !savedText.contentEquals(field.text),
            canUndo = history.canUndo,
            canRedo = history.canRedo,
        )
    }

    private fun save(thenClose: Boolean) {
        val current = document ?: return
        if (!editable() || _state.value.saving) return
        record()
        val text = field.text.toString()
        val bytes = current.encode(text)
        if (bytes == null) {
            _state.update { it.copy(error = EditorError.ENCODING) }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val result = repository.stat(path).fold({ repository.replaceContent(it, bytes) }, { Result.failure(it) })
            if (result.isSuccess) {
                savedText = text
                _state.update { it.copy(saving = false, recovered = false, restoredDraft = false, closed = thenClose) }
                refresh()
                persist()
            } else {
                _state.update { it.copy(saving = false, error = EditorError.SAVE) }
            }
        }
    }
}

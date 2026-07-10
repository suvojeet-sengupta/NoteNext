package com.suvojeet.notenext.ui.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.data.NoteRepository
import com.suvojeet.notenext.data.share.ShareRepository
import com.suvojeet.notenext.util.HtmlConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

data class SharedNoteUiState(
    val loading: Boolean = true,
    val error: String? = null,
    /** True when the note is gone for good (expired / burned / missing) — no point retrying. */
    val gone: Boolean = false,
    val title: String = "",
    val content: String = "",          // plain text shown read-only
    val contentHtml: String = "",      // original HTML (used when saving a copy)
    val sharedBy: String = "NoteNext user",
    val createdAt: String? = null,
    val expiresAt: String? = null,
    val burnAfterRead: Boolean = false,
    val savedLocally: Boolean = false
)

sealed interface SharedNoteEvent {
    data class Toast(val message: String) : SharedNoteEvent
    object SavedCopy : SharedNoteEvent
}

/**
 * Loads an end-to-end encrypted shared note, decrypts it on-device with the key
 * from the link fragment, and shows it read-only. There is no live collaboration:
 * shares are ephemeral secrets the server cannot read, so they cannot be co-edited.
 */
@HiltViewModel
class SharedNoteViewModel @Inject constructor(
    private val shareRepository: ShareRepository,
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SharedNoteUiState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<SharedNoteEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    private var shareId: String? = null
    private var keyFragment: String? = null
    private var started = false

    fun start(id: String, key: String?) {
        if (started) return
        started = true
        shareId = id
        keyFragment = key?.trim()?.takeIf { it.isNotEmpty() }
        load()
    }

    private fun load() {
        val id = shareId ?: return
        val key = keyFragment
        if (key == null) {
            _state.update {
                it.copy(
                    loading = false,
                    gone = true,
                    error = "This link is missing its decryption key, so the note can't be opened. Ask the sender for the full link."
                )
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, gone = false) }
            shareRepository.getNote(id, key)
                .onSuccess { note ->
                    val plain = if (note.content.isNotBlank()) HtmlConverter.htmlToPlainText(note.content) else ""
                    _state.update {
                        it.copy(
                            loading = false,
                            error = null,
                            gone = false,
                            title = note.title,
                            content = plain,
                            contentHtml = note.content,
                            sharedBy = note.sharedBy,
                            createdAt = note.createdAt,
                            expiresAt = note.expiresAt,
                            burnAfterRead = note.burnAfterRead || note.burned
                        )
                    }
                }
                .onFailure { t ->
                    val code = (t as? HttpException)?.code()
                    val gone = code == 410 || code == 404
                    val message = when (code) {
                        410 -> "This note has expired or was already opened, and is no longer available."
                        404 -> "This note doesn't exist. The link may be incorrect."
                        else -> "Couldn't open this note. It may be corrupted, or the link may be incomplete."
                    }
                    _state.update { it.copy(loading = false, gone = gone, error = message) }
                }
        }
    }

    /** Saves the current shared note as a new local note in the user's own library. */
    fun saveCopy() {
        viewModelScope.launch {
            val snapshot = _state.value
            val now = System.currentTimeMillis()
            val note = Note(
                title = snapshot.title,
                content = snapshot.contentHtml.ifBlank { plainToHtml(snapshot.content) },
                createdAt = now,
                lastEdited = now,
                color = 0
            )
            runCatching { noteRepository.insertNote(note) }
                .onSuccess {
                    _state.update { it.copy(savedLocally = true) }
                    _events.tryEmit(SharedNoteEvent.SavedCopy)
                    _events.tryEmit(SharedNoteEvent.Toast("Saved to your notes"))
                }
                .onFailure { _events.tryEmit(SharedNoteEvent.Toast("Couldn't save a copy")) }
        }
    }

    fun retry() {
        if (_state.value.gone) return
        load()
    }
}

/** Escapes plain text and converts newlines to <br> so a saved copy keeps its line breaks. */
private fun plainToHtml(text: String): String =
    text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\n", "<br>")

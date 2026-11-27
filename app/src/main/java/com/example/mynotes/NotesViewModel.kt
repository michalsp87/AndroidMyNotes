package com.example.mynotes

import android.app.Application
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class NoteDraft(val id: Long?, val title: String, val body: String)

internal class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val database = NotesDatabase.getInstance(application)

    var notes by mutableStateOf<List<Note>>(emptyList())
        private set
    var draft by mutableStateOf<NoteDraft?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var loaded by mutableStateOf(false)
        private set
    var confirmDelete by mutableStateOf(false)
        private set
    var error by mutableStateOf<Int?>(null)
        private set

    init {
        load()
    }

    fun load() = perform(R.string.notes_load_error) {
        notes = withContext(Dispatchers.IO) { database.notes() }
        loaded = true
    }

    fun edit(note: Note? = null) {
        draft = NoteDraft(note?.id, note?.title.orEmpty(), note?.body.orEmpty())
    }

    fun update(title: String, body: String) {
        draft = draft?.copy(title = title, body = body)
    }

    fun closeEditor() {
        draft = null
    }

    fun confirmDeletion(show: Boolean) {
        confirmDelete = show
    }

    fun dismissError() {
        error = null
    }

    fun save() {
        val current = draft ?: return
        if (current.title.isBlank() && current.body.isBlank()) return
        perform(R.string.notes_save_error) {
            val saved = withContext(Dispatchers.IO) {
                database.save(current.id, current.title.trim(), current.body)
            }
            notes = (notes.filterNot { it.id == saved.id } + saved)
                .sortedWith(compareByDescending<Note> { it.updatedAt }.thenByDescending { it.id })
            closeEditor()
        }
    }

    fun delete() {
        val id = draft?.id ?: return
        confirmDeletion(false)
        perform(R.string.notes_delete_error) {
            withContext(Dispatchers.IO) { database.delete(id) }
            notes = notes.filterNot { it.id == id }
            closeEditor()
        }
    }

    private fun perform(@StringRes message: Int, action: suspend () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                action()
            } catch (exception: SQLiteException) {
                Log.e("MyNotes", "Notes database operation failed", exception)
                error = message
            } finally {
                busy = false
            }
        }
    }
}

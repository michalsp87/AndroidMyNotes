package com.example.mynotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mynotes.ui.theme.MyNotesTheme

class MainActivity : ComponentActivity() {
    private val notesModel by viewModels<NotesViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyNotesTheme {
                NotesScreen(notesModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesScreen(model: NotesViewModel) {
    val draft = model.draft
    val snackbar = remember { SnackbarHostState() }
    val errorMessage = model.error?.let { stringResource(it) }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbar.showSnackbar(errorMessage)
            model.dismissError()
        }
    }

    BackHandler(enabled = draft != null) {
        if (!model.busy) model.closeEditor()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        topBar = {
            TopAppBar(title = {
                Text(stringResource(when {
                    draft == null -> R.string.app_name
                    draft.id == null -> R.string.note_new
                    else -> R.string.note_edit
                }))
            })
        },
        floatingActionButton = {
            if (draft == null && model.loaded && !model.busy) {
                ExtendedFloatingActionButton(onClick = { model.edit() }) {
                    Text(stringResource(R.string.note_new))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val content = Modifier.fillMaxSize().padding(padding)
        when {
            draft != null -> NoteEditor(draft, model, content)
            !model.loaded -> Box(content, contentAlignment = Alignment.Center) {
                if (model.busy) {
                    CircularProgressIndicator()
                } else {
                    Button(onClick = model::load) { Text(stringResource(R.string.notes_retry)) }
                }
            }
            model.notes.isEmpty() -> Box(content, contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.notes_empty))
            }
            else -> NotesList(model.notes, model::edit, content)
        }
    }

    if (model.confirmDelete) {
        AlertDialog(
            onDismissRequest = { model.confirmDeletion(false) },
            title = { Text(stringResource(R.string.note_delete_title)) },
            text = { Text(stringResource(R.string.note_delete_message)) },
            confirmButton = {
                TextButton(onClick = model::delete) { Text(stringResource(R.string.note_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { model.confirmDeletion(false) }) {
                    Text(stringResource(R.string.note_cancel))
                }
            },
        )
    }
}

@Composable
private fun NotesList(notes: List<Note>, onEdit: (Note) -> Unit, modifier: Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(notes, key = { it.id }) { note ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onEdit(note) }) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = note.title.ifBlank { stringResource(R.string.note_untitled) },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (note.body.isNotBlank()) {
                        Text(note.body, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteEditor(draft: NoteDraft, model: NotesViewModel, modifier: Modifier) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = draft.title,
            onValueChange = { model.update(it, draft.body) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.note_title)) },
            singleLine = true,
            enabled = !model.busy,
        )
        OutlinedTextField(
            value = draft.body,
            onValueChange = { model.update(draft.title, it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.note_body)) },
            minLines = 5,
            enabled = !model.busy,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = model::closeEditor, enabled = !model.busy) {
                Text(stringResource(R.string.note_cancel))
            }
            Button(
                onClick = model::save,
                enabled = !model.busy && (draft.title.isNotBlank() || draft.body.isNotBlank()),
            ) {
                Text(stringResource(R.string.note_save))
            }
        }
        if (draft.id != null) {
            TextButton(onClick = { model.confirmDeletion(true) }, enabled = !model.busy) {
                Text(stringResource(R.string.note_delete), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

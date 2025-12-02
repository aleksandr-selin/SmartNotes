package ru.selin.smartnotes.presentation.screens.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.jetbrains.compose.resources.stringResource
import ru.selin.smartnotes.domain.model.Note
import smartnotes.composeapp.generated.resources.Res
import smartnotes.composeapp.generated.resources.note_add
import smartnotes.composeapp.generated.resources.notes_empty
import smartnotes.composeapp.generated.resources.notes_title
import smartnotes.composeapp.generated.resources.retry

/** Экран списка заметок */
class NotesListScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = getScreenModel<NotesListViewModel>()
        val uiState by viewModel.uiState.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.notes_title)) }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { navigator.push(NoteDetailScreen(noteId = null)) }
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(Res.string.note_add)
                    )
                }
            }
        ) { paddingValues ->
            when (val state = uiState) {
                is NotesListUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is NotesListUiState.Empty -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.notes_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is NotesListUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 160.dp // Отступ для FAB + NavigationBar
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = state.notes,
                            key = { it.id }
                        ) { note ->
                            NoteCard(
                                note = note,
                                onClick = { navigator.push(NoteDetailScreen(note.id)) },
                                onDelete = { viewModel.deleteNote(note.id) }
                            )
                        }
                    }
                }

                is NotesListUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Ошибка: ${state.message}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = { viewModel.loadNotes() }) {
                                Text(stringResource(Res.string.retry))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Карточка заметки
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


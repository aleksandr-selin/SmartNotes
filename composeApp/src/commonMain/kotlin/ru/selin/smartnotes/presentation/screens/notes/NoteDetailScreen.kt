package ru.selin.smartnotes.presentation.screens.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf
import smartnotes.composeapp.generated.resources.Res
import smartnotes.composeapp.generated.resources.back
import smartnotes.composeapp.generated.resources.cancel
import smartnotes.composeapp.generated.resources.delete
import smartnotes.composeapp.generated.resources.error_format
import smartnotes.composeapp.generated.resources.note_content_hint
import smartnotes.composeapp.generated.resources.note_delete_confirm
import smartnotes.composeapp.generated.resources.note_delete_confirm_message
import smartnotes.composeapp.generated.resources.note_edit
import smartnotes.composeapp.generated.resources.note_new
import smartnotes.composeapp.generated.resources.note_title_hint
import smartnotes.composeapp.generated.resources.save

/**
 * Экран детальной информации заметки
 *
 * @param noteId ID заметки для редактирования, null для создания новой
 */
data class NoteDetailScreen(val noteId: Long?) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = getScreenModel<NoteDetailViewModel> { parametersOf(noteId) }
        val uiState by viewModel.uiState.collectAsState()
        val title by viewModel.title.collectAsState()
        val content by viewModel.content.collectAsState()
        val isSaving by viewModel.isSaving.collectAsState()
        val canSave by viewModel.canSave.collectAsState()

        var showDeleteDialog by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (viewModel.isEditMode) {
                                stringResource(Res.string.note_edit)
                            } else {
                                stringResource(Res.string.note_new)
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(Res.string.back)
                            )
                        }
                    },
                    actions = {
                        // Кнопка удаления (только в режиме редактирования)
                        if (viewModel.isEditMode) {
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(Res.string.delete),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        
                        // Кнопка сохранения
                        IconButton(
                            onClick = {
                                viewModel.saveNote {
                                    navigator.pop()
                                }
                            },
                            enabled = canSave && !isSaving
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = stringResource(Res.string.save)
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            when (val state = uiState) {
                is NoteDetailUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is NoteDetailUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = viewModel::updateTitle,
                            label = { Text(stringResource(Res.string.note_title_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = content,
                            onValueChange = viewModel::updateContent,
                            label = { Text(stringResource(Res.string.note_content_hint)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            minLines = 10
                        )

                        if (isSaving) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                is NoteDetailUiState.Error -> {
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
                                text = stringResource(Res.string.error_format, state.message),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = { navigator.pop() }) {
                                Text(stringResource(Res.string.back))
                            }
                        }
                    }
                }
            }
        }

        // Диалог подтверждения удаления
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(Res.string.note_delete_confirm)) },
                text = { Text(stringResource(Res.string.note_delete_confirm_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.deleteNote {
                                navigator.pop()
                            }
                        }
                    ) {
                        Text(
                            stringResource(Res.string.delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(Res.string.cancel))
                    }
                }
            )
        }
    }
}

package ru.selin.smartnotes.presentation.screens.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import ru.selin.smartnotes.domain.model.Importance
import ru.selin.smartnotes.domain.model.Subtask
import smartnotes.composeapp.generated.resources.Res
import smartnotes.composeapp.generated.resources.*

/**
 * Экран детальной информации задачи
 *
 * @param taskId ID задачи для редактирования, null для создания новой
 */
data class TaskDetailScreen(val taskId: Long?) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = getScreenModel<TaskDetailViewModel> { parametersOf(taskId) }
        val uiState by viewModel.uiState.collectAsState()
        val title by viewModel.title.collectAsState()
        val description by viewModel.description.collectAsState()
        val importance by viewModel.importance.collectAsState()
        val isToday by viewModel.isToday.collectAsState()
        val isCompleted by viewModel.isCompleted.collectAsState()
        val subtasks by viewModel.subtasks.collectAsState()
        val isSaving by viewModel.isSaving.collectAsState()
        val canSave by viewModel.canSave.collectAsState()

        var showSubtaskDialog by remember { mutableStateOf(false) }
        var showDeleteDialog by remember { mutableStateOf(false) }
        var showCompletionDialog by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(if (viewModel.isEditMode) stringResource(Res.string.task_edit) else stringResource(Res.string.task_new))
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
                                viewModel.saveTask {
                                    navigator.pop()
                                }
                            },
                            enabled = canSave && !isSaving
                        ) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(Res.string.save))
                        }
                    }
                )
            }
        ) { paddingValues ->
            when (val state = uiState) {
                is TaskDetailUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is TaskDetailUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Заголовок
                        item {
                            OutlinedTextField(
                                value = title,
                                onValueChange = viewModel::updateTitle,
                                label = { Text(stringResource(Res.string.task_title_hint)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        // Описание
                        item {
                            OutlinedTextField(
                                value = description,
                                onValueChange = viewModel::updateDescription,
                                label = { Text(stringResource(Res.string.task_description_hint)) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )
                        }

                        // Важность
                        item {
                            Text(
                                text = stringResource(Res.string.importance_label),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Importance.entries.forEach { imp ->
                                    FilterChip(
                                        selected = importance == imp,
                                        onClick = { viewModel.updateImportance(imp) },
                                        label = { Text(getImportanceText(imp)) }
                                    )
                                }
                            }
                        }

                        // Сегодня
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(Res.string.task_do_today))
                                Switch(
                                    checked = isToday,
                                    onCheckedChange = { viewModel.toggleIsToday() }
                                )
                            }
                        }

                        // Задача выполнена (только в режиме редактирования)
                        if (viewModel.isEditMode) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stringResource(Res.string.task_completed))
                                    Checkbox(
                                        checked = isCompleted,
                                        onCheckedChange = {
                                            viewModel.toggleTaskCompletion {
                                                showCompletionDialog = true
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Подзадачи
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(Res.string.subtasks_label),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                IconButton(onClick = { showSubtaskDialog = true }) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = stringResource(Res.string.subtask_add)
                                    )
                                }
                            }
                        }

                        items(
                            items = subtasks,
                            key = { it.id }
                        ) { subtask ->
                            SubtaskItem(
                                subtask = subtask,
                                onToggle = { viewModel.toggleSubtask(subtask.id) },
                                onDelete = { viewModel.deleteSubtask(subtask.id) }
                            )
                        }

                        // Индикатор сохранения
                        if (isSaving) {
                            item {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                is TaskDetailUiState.Error -> {
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

        // Диалог добавления подзадачи
        if (showSubtaskDialog) {
            AddSubtaskDialog(
                onDismiss = { showSubtaskDialog = false },
                onAdd = { title ->
                    viewModel.addSubtask(title)
                    showSubtaskDialog = false
                }
            )
        }

        // Диалог подтверждения удаления задачи
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(Res.string.task_delete_confirm)) },
                text = { Text(stringResource(Res.string.task_delete_confirm_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.deleteTask {
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

        // Диалог завершения задачи (удалить или оставить)
        if (showCompletionDialog) {
            AlertDialog(
                onDismissRequest = { showCompletionDialog = false },
                title = { Text(stringResource(Res.string.task_complete_dialog_title)) },
                text = { Text(stringResource(Res.string.task_complete_dialog_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showCompletionDialog = false
                            viewModel.completeTask(shouldDelete = true) {
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
                    TextButton(
                        onClick = {
                            showCompletionDialog = false
                            viewModel.completeTask(shouldDelete = false) {
                                // Остаемся на экране, задача просто отмечена выполненной
                            }
                        }
                    ) {
                        Text(stringResource(Res.string.task_keep))
                    }
                }
            )
        }
    }
}

/**
 * Элемент подзадачи
 */
@Composable
private fun SubtaskItem(
    subtask: Subtask,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = subtask.isDone,
                onCheckedChange = { onToggle() }
            )

            Text(
                text = subtask.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(Res.string.delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Диалог добавления подзадачи
 */
@Composable
private fun AddSubtaskDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.subtask_new)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(Res.string.subtask_name_hint)) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(text) },
                enabled = text.isNotBlank()
            ) {
                Text(stringResource(Res.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

@Composable
private fun getImportanceText(importance: Importance): String {
    return when (importance) {
        Importance.LOW -> stringResource(Res.string.importance_low)
        Importance.MEDIUM -> stringResource(Res.string.importance_medium)
        Importance.HIGH -> stringResource(Res.string.importance_high)
    }
}

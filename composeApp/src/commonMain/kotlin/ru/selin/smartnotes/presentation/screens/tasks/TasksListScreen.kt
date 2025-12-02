package ru.selin.smartnotes.presentation.screens.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import ru.selin.smartnotes.domain.model.Task
import smartnotes.composeapp.generated.resources.Res
import smartnotes.composeapp.generated.resources.*

/**
 * Экран списка задач с фильтрами
 */
class TasksListScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = getScreenModel<TasksListViewModel>()
        val uiState by viewModel.uiState.collectAsState()
        val currentFilter by viewModel.currentFilter.collectAsState()

        var showCompletionDialog by remember { mutableStateOf(false) }
        var taskToComplete by remember { mutableStateOf<Long?>(null) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.tasks_title)) }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { navigator.push(TaskDetailScreen(taskId = null)) }
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.task_add))
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Фильтры
                FilterChips(
                    currentFilter = currentFilter,
                    onFilterSelected = viewModel::loadTasks
                )

                // Список задач
                when (val state = uiState) {
                    is TasksListUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is TasksListUiState.Empty -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = getEmptyMessage(state.filter),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    is TasksListUiState.Success -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 16.dp,
                                bottom = 160.dp // Отступ для FAB + NavigationBar
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = state.tasks,
                                key = { it.id }
                            ) { task ->
                                TaskCard(
                                    task = task,
                                    onClick = { navigator.push(TaskDetailScreen(task.id)) },
                                    onToggleComplete = {
                                        viewModel.toggleTaskCompletion(
                                            taskId = task.id,
                                            currentIsCompleted = task.isCompleted,
                                            onShowCompletionDialog = { taskId ->
                                                taskToComplete = taskId
                                                showCompletionDialog = true
                                            }
                                        )
                                    },
                                    onToggleSubtask = { subtaskId, isDone ->
                                        viewModel.toggleSubtask(
                                            taskId = task.id,
                                            subtaskId = subtaskId,
                                            isDone = isDone,
                                            onShowCompletionDialog = { taskId ->
                                                taskToComplete = taskId
                                                showCompletionDialog = true
                                            }
                                        )
                                    },
                                    isProcessing = taskToComplete == task.id && showCompletionDialog
                                )
                            }
                        }
                    }

                    is TasksListUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
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
                                Button(onClick = { viewModel.loadTasks() }) {
                                    Text(stringResource(Res.string.retry))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Диалог завершения задачи (удалить или оставить)
        if (showCompletionDialog && taskToComplete != null) {
            AlertDialog(
                onDismissRequest = { 
                    showCompletionDialog = false
                    taskToComplete = null
                },
                title = { Text(stringResource(Res.string.task_complete_dialog_title)) },
                text = { Text(stringResource(Res.string.task_complete_dialog_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            taskToComplete?.let { taskId ->
                                viewModel.completeTask(taskId, shouldDelete = true)
                            }
                            showCompletionDialog = false
                            taskToComplete = null
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
                            taskToComplete?.let { taskId ->
                                viewModel.completeTask(taskId, shouldDelete = false)
                            }
                            showCompletionDialog = false
                            taskToComplete = null
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
 * Фильтры задач
 */
@Composable
private fun FilterChips(
    currentFilter: TasksFilter,
    onFilterSelected: (TasksFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TasksFilter.entries.forEach { filter ->
            FilterChip(
                selected = currentFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.displayName) }
            )
        }
    }
}

/**
 * Карточка задачи
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
    onToggleSubtask: (subtaskId: Long, isDone: Boolean) -> Unit,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    var showAllSubtasks by remember { mutableStateOf(false) }
    
    // Сортируем подзадачи: невыполненные сначала, выполненные в конце
    val sortedSubtasks = remember(task.subtasks) {
        task.subtasks.sortedBy { it.isDone }
    }
    
    // Определяем какие подзадачи показывать
    val visibleSubtasks = remember(sortedSubtasks, showAllSubtasks) {
        if (showAllSubtasks || sortedSubtasks.size <= 3) {
            sortedSubtasks
        } else {
            sortedSubtasks.take(3)
        }
    }
    
    val hiddenCount = sortedSubtasks.size - visibleSubtasks.size

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Заголовок задачи с чекбоксом и важностью
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggleComplete() },
                    enabled = !isProcessing
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (task.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Индикатор важности
                if (task.importance != ru.selin.smartnotes.domain.model.Importance.LOW) {
                    Badge {
                        Text(
                            when (task.importance) {
                                ru.selin.smartnotes.domain.model.Importance.HIGH -> "!!"
                                ru.selin.smartnotes.domain.model.Importance.MEDIUM -> "!"
                                else -> ""
                            }
                        )
                    }
                }
            }

            // Подзадачи
            if (task.subtasks.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    visibleSubtasks.forEach { subtask ->
                        SubtaskRow(
                            subtask = subtask,
                            onToggle = { onToggleSubtask(subtask.id, !subtask.isDone) }
                        )
                    }

                    // Кнопка "Ещё n" или "Свернуть"
                    if (sortedSubtasks.size > 3) {
                        TextButton(
                            onClick = { showAllSubtasks = !showAllSubtasks },
                            modifier = Modifier.padding(start = 32.dp)
                        ) {
                            Text(
                                text = if (showAllSubtasks) {
                                    "Свернуть"
                                } else {
                                    "Ещё $hiddenCount"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Строка подзадачи
 */
@Composable
private fun SubtaskRow(
    subtask: ru.selin.smartnotes.domain.model.Subtask,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = subtask.isDone,
            onCheckedChange = { onToggle() }
        )
        Text(
            text = subtask.title,
            style = MaterialTheme.typography.bodySmall,
            color = if (subtask.isDone) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun getEmptyMessage(filter: TasksFilter): String {
    return when (filter) {
        TasksFilter.ALL -> stringResource(Res.string.tasks_empty)
        TasksFilter.TODAY -> stringResource(Res.string.tasks_empty_today)
        TasksFilter.ACTIVE -> stringResource(Res.string.tasks_empty_active)
        TasksFilter.COMPLETED -> stringResource(Res.string.tasks_empty_completed)
    }
}

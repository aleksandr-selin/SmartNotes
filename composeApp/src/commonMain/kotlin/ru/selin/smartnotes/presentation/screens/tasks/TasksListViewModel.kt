package ru.selin.smartnotes.presentation.screens.tasks

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.selin.smartnotes.domain.model.Task
import ru.selin.smartnotes.domain.usecase.subtasks.ToggleSubtaskUseCase
import ru.selin.smartnotes.domain.usecase.tasks.CompleteTaskUseCase
import ru.selin.smartnotes.domain.usecase.tasks.DeleteTaskUseCase
import ru.selin.smartnotes.domain.usecase.tasks.GetAllTasksUseCase
import ru.selin.smartnotes.domain.usecase.tasks.GetTasksForTodayUseCase
import ru.selin.smartnotes.domain.usecase.tasks.TaskFilter

/**
 * ViewModel для экрана списка задач
 *
 * Поддерживает фильтрацию: All, Today, Active, Completed
 */
class TasksListViewModel(
    private val getAllTasksUseCase: GetAllTasksUseCase,
    private val getTasksForTodayUseCase: GetTasksForTodayUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val toggleSubtaskUseCase: ToggleSubtaskUseCase
) : ScreenModel {

    private val _uiState = MutableStateFlow<TasksListUiState>(TasksListUiState.Loading)
    val uiState: StateFlow<TasksListUiState> = _uiState.asStateFlow()

    private val _currentFilter = MutableStateFlow(TasksFilter.ALL)
    val currentFilter: StateFlow<TasksFilter> = _currentFilter.asStateFlow()

    init {
        loadTasks()
    }

    /**
     * Загружает задачи с текущим фильтром
     */
    fun loadTasks(filter: TasksFilter = _currentFilter.value) {
        _currentFilter.value = filter

        screenModelScope.launch {
            _uiState.value = TasksListUiState.Loading

            val flow = when (filter) {
                TasksFilter.ALL -> getAllTasksUseCase(TaskFilter.ALL)
                TasksFilter.TODAY -> getTasksForTodayUseCase()
                TasksFilter.ACTIVE -> getAllTasksUseCase(TaskFilter.ACTIVE)
                TasksFilter.COMPLETED -> getAllTasksUseCase(TaskFilter.COMPLETED)
            }

            flow.collect { tasks ->
                _uiState.value = if (tasks.isEmpty()) {
                    TasksListUiState.Empty(filter)
                } else {
                    TasksListUiState.Success(tasks, filter)
                }
            }
        }
    }

    /**
     * Переключает статус завершения задачи
     * При отметке как выполненной - вызывается onShowCompletionDialog
     * При снятии отметки - снимает флаг напрямую
     */
    fun toggleTaskCompletion(taskId: Long, currentIsCompleted: Boolean, onShowCompletionDialog: (Long) -> Unit) {
        if (!currentIsCompleted) {
            // Помечаем как выполненную - показываем диалог
            onShowCompletionDialog(taskId)
        } else {
            // Снимаем отметку выполнения - делаем напрямую
            screenModelScope.launch {
                completeTaskUseCase(
                    taskId = taskId,
                    shouldDelete = false
                ).fold(
                    onSuccess = {
                        // Список обновится автоматически через Flow
                    },
                    onFailure = { error ->
                        _uiState.value = TasksListUiState.Error(
                            error.message ?: "Ошибка обновления"
                        )
                    }
                )
            }
        }
    }

    /**
     * Завершает задачу с выбором: удалить или оставить
     */
    fun completeTask(taskId: Long, shouldDelete: Boolean) {
        screenModelScope.launch {
            completeTaskUseCase(
                taskId = taskId,
                shouldDelete = shouldDelete
            ).fold(
                onSuccess = {
                    // Список обновится автоматически через Flow
                },
                onFailure = { error ->
                    _uiState.value = TasksListUiState.Error(
                        error.message ?: "Ошибка завершения"
                    )
                }
            )
        }
    }

    /**
     * Удаляет задачу
     */
    fun deleteTask(taskId: Long) {
        screenModelScope.launch {
            deleteTaskUseCase(taskId).fold(
                onSuccess = {
                    // Список обновится автоматически через Flow
                },
                onFailure = { error ->
                    _uiState.value = TasksListUiState.Error(
                        error.message ?: "Ошибка удаления"
                    )
                }
            )
        }
    }

    /**
     * Переключает статус подзадачи
     * Автоматически завершает задачу, если все подзадачи выполнены
     */
    fun toggleSubtask(taskId: Long, subtaskId: Long, isDone: Boolean, onShowCompletionDialog: (Long) -> Unit) {
        screenModelScope.launch {
            toggleSubtaskUseCase(
                subtaskId = subtaskId,
                isDone = isDone
            ).fold(
                onSuccess = {
                    // Проверяем, нужно ли завершить задачу
                    checkAndCompleteTaskIfNeeded(taskId, onShowCompletionDialog)
                },
                onFailure = { error ->
                    _uiState.value = TasksListUiState.Error(
                        error.message ?: "Ошибка обновления подзадачи"
                    )
                }
            )
        }
    }

    /**
     * Проверяет, выполнены ли все подзадачи, и если да - показывает диалог завершения
     */
    private suspend fun checkAndCompleteTaskIfNeeded(taskId: Long, onShowCompletionDialog: (Long) -> Unit) {
        val state = _uiState.value
        if (state is TasksListUiState.Success) {
            val task = state.tasks.find { it.id == taskId }
            if (task != null && task.subtasks.isNotEmpty() && task.subtasks.all { it.isDone } && !task.isCompleted) {
                // Все подзадачи выполнены, задача не завершена - показываем диалог
                onShowCompletionDialog(taskId)
            }
        }
    }
}

/**
 * UI состояния для экрана списка задач
 */
sealed class TasksListUiState {
    data object Loading : TasksListUiState()
    data class Empty(val filter: TasksFilter) : TasksListUiState()
    data class Success(val tasks: List<Task>, val filter: TasksFilter) : TasksListUiState()
    data class Error(val message: String) : TasksListUiState()
}

/**
 * Фильтры для списка задач
 */
enum class TasksFilter(val displayNameKey: String) {
    ALL("Все"),
    TODAY("Сегодня"),
    ACTIVE("Активные"),
    COMPLETED("Завершённые");

    // Для обратной совместимости, можно будет удалить позже
    val displayName: String get() = displayNameKey
}


package ru.selin.smartnotes.presentation.screens.tasks

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.selin.smartnotes.domain.model.Importance
import ru.selin.smartnotes.domain.model.Subtask
import ru.selin.smartnotes.domain.usecase.subtasks.AddSubtaskUseCase
import ru.selin.smartnotes.domain.usecase.subtasks.DeleteSubtaskUseCase
import ru.selin.smartnotes.domain.usecase.subtasks.ToggleSubtaskUseCase
import ru.selin.smartnotes.domain.usecase.tasks.AddTaskUseCase
import ru.selin.smartnotes.domain.usecase.tasks.CompleteTaskUseCase
import ru.selin.smartnotes.domain.usecase.tasks.DeleteTaskUseCase
import ru.selin.smartnotes.domain.usecase.tasks.GetTaskByIdUseCase
import ru.selin.smartnotes.domain.usecase.tasks.UpdateTaskUseCase

/**
 * ViewModel для экрана детальной информации задачи
 *
 * Поддерживает создание и редактирование задачи с подзадачами
 */
class TaskDetailViewModel(
    private val taskId: Long?,
    private val getTaskByIdUseCase: GetTaskByIdUseCase,
    private val addTaskUseCase: AddTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val addSubtaskUseCase: AddSubtaskUseCase,
    private val toggleSubtaskUseCase: ToggleSubtaskUseCase,
    private val deleteSubtaskUseCase: DeleteSubtaskUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase
) : ScreenModel {

    private val _uiState = MutableStateFlow<TaskDetailUiState>(TaskDetailUiState.Loading)
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _importance = MutableStateFlow(Importance.MEDIUM)
    val importance: StateFlow<Importance> = _importance.asStateFlow()

    private val _isToday = MutableStateFlow(false)
    val isToday: StateFlow<Boolean> = _isToday.asStateFlow()

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted.asStateFlow()

    private val _subtasks = MutableStateFlow<List<Subtask>>(emptyList())
    val subtasks: StateFlow<List<Subtask>> = _subtasks.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _canSave = MutableStateFlow(false)
    val canSave: StateFlow<Boolean> = _canSave.asStateFlow()

    val isEditMode: Boolean = taskId != null

    init {
        if (taskId != null) {
            loadTask(taskId)
        } else {
            _uiState.value = TaskDetailUiState.Success
        }
    }

    /**
     * Загружает задачу по ID
     */
    private fun loadTask(id: Long) {
        screenModelScope.launch {
            _uiState.value = TaskDetailUiState.Loading

            try {
                val task = getTaskByIdUseCase(id)
                if (task != null) {
                    _title.value = task.title
                    _description.value = task.description
                    _importance.value = task.importance
                    _isToday.value = task.isToday
                    _isCompleted.value = task.isCompleted
                    _subtasks.value = task.subtasks
                    updateCanSave()
                    _uiState.value = TaskDetailUiState.Success
                } else {
                    _uiState.value = TaskDetailUiState.Error("Задача не найдена")
                }
            } catch (e: Exception) {
                _uiState.value = TaskDetailUiState.Error(
                    e.message ?: "Ошибка загрузки"
                )
            }
        }
    }

    /**
     * Обновляет заголовок задачи
     */
    fun updateTitle(newTitle: String) {
        _title.value = newTitle
        updateCanSave()
    }

    /**
     * Обновляет описание задачи
     */
    fun updateDescription(newDescription: String) {
        _description.value = newDescription
    }

    /**
     * Обновляет важность задачи
     */
    fun updateImportance(newImportance: Importance) {
        _importance.value = newImportance
    }

    /**
     * Переключает флаг "Сегодня"
     */
    fun toggleIsToday() {
        _isToday.value = !_isToday.value
    }

    /**
     * Обновляет состояние возможности сохранения
     */
    private fun updateCanSave() {
        _canSave.value = _title.value.isNotBlank()
    }

    /**
     * Добавляет подзадачу
     */
    fun addSubtask(subtaskTitle: String) {
        if (subtaskTitle.isBlank()) return

        screenModelScope.launch {
            if (isEditMode && taskId != null) {
                // Режим редактирования - добавляем в БД
                addSubtaskUseCase(
                    taskId = taskId,
                    title = subtaskTitle
                ).fold(
                    onSuccess = { subtaskId ->
                        // Перезагружаем задачу для обновления списка подзадач
                        loadTask(taskId)
                    },
                    onFailure = { error ->
                        _uiState.value = TaskDetailUiState.Error(
                            error.message ?: "Ошибка добавления подзадачи"
                        )
                    }
                )
            } else {
                // Режим создания - добавляем в локальный список
                val newSubtask = Subtask(
                    id = -(_subtasks.value.size + 1).toLong(), // Временный ID
                    taskId = -1,
                    title = subtaskTitle,
                    isDone = false
                )
                _subtasks.value = _subtasks.value + newSubtask
            }
        }
    }

    /**
     * Переключает статус подзадачи
     */
    fun toggleSubtask(subtaskId: Long) {
        screenModelScope.launch {
            if (isEditMode && taskId != null) {
                val subtask = _subtasks.value.find { it.id == subtaskId } ?: return@launch

                toggleSubtaskUseCase(
                    subtaskId = subtaskId,
                    isDone = !subtask.isDone
                ).fold(
                    onSuccess = {
                        // Перезагружаем задачу
                        loadTask(taskId)
                    },
                    onFailure = { error ->
                        _uiState.value = TaskDetailUiState.Error(
                            error.message ?: "Ошибка обновления подзадачи"
                        )
                    }
                )
            } else {
                // Режим создания - обновляем локально
                _subtasks.value = _subtasks.value.map {
                    if (it.id == subtaskId) it.copy(isDone = !it.isDone) else it
                }
                // Проверяем, выполнены ли все подзадачи
                checkAndUpdateTaskCompletion()
            }
        }
    }

    /**
     * Проверяет, выполнены ли все подзадачи, и автоматически отмечает задачу выполненной
     */
    private fun checkAndUpdateTaskCompletion() {
        if (_subtasks.value.isNotEmpty() && _subtasks.value.all { it.isDone }) {
            _isCompleted.value = true
        }
    }

    /**
     * Удаляет подзадачу
     */
    fun deleteSubtask(subtaskId: Long) {
        screenModelScope.launch {
            if (isEditMode && taskId != null) {
                deleteSubtaskUseCase(subtaskId).fold(
                    onSuccess = {
                        // Перезагружаем задачу
                        loadTask(taskId)
                    },
                    onFailure = { error ->
                        _uiState.value = TaskDetailUiState.Error(
                            error.message ?: "Ошибка удаления подзадачи"
                        )
                    }
                )
            } else {
                // Режим создания - удаляем локально
                _subtasks.value = _subtasks.value.filter { it.id != subtaskId }
            }
        }
    }

    /**
     * Сохраняет задачу (создание или обновление)
     */
    fun saveTask(onSuccess: () -> Unit) {
        if (_isSaving.value) return

        screenModelScope.launch {
            _isSaving.value = true

            val result = if (isEditMode && taskId != null) {
                // Режим редактирования
                updateTaskUseCase(
                    taskId = taskId,
                    title = _title.value,
                    description = _description.value,
                    importance = _importance.value,
                    isToday = _isToday.value
                )
            } else {
                // Режим создания
                addTaskUseCase(
                    title = _title.value,
                    description = _description.value,
                    importance = _importance.value,
                    isToday = _isToday.value,
                    subtasks = _subtasks.value.map { it.title }
                )
            }

            result.fold(
                onSuccess = {
                    _isSaving.value = false
                    onSuccess()
                },
                onFailure = { error ->
                    _isSaving.value = false
                    _uiState.value = TaskDetailUiState.Error(
                        error.message ?: "Ошибка сохранения"
                    )
                }
            )
        }
    }

    /**
     * Проверяет, можно ли сохранить задачу
     * @deprecated Используйте canSave StateFlow вместо этого метода
     */
    @Deprecated("Use canSave StateFlow instead", ReplaceWith("canSave.value"))
    fun canSave(): Boolean {
        return _title.value.isNotBlank()
    }

    /**
     * Переключает статус выполнения задачи
     * При отметке как выполненной - вызывается onShowCompletionDialog
     */
    fun toggleTaskCompletion(onShowCompletionDialog: () -> Unit) {
        if (!isEditMode || taskId == null) {
            // В режиме создания просто переключаем локально
            _isCompleted.value = !_isCompleted.value
            return
        }

        if (!_isCompleted.value) {
            // Помечаем как выполненную - показываем диалог
            onShowCompletionDialog()
        } else {
            // Снимаем отметку выполнения
            _isCompleted.value = false
        }
    }

    /**
     * Завершает задачу с выбором: удалить или оставить
     */
    fun completeTask(shouldDelete: Boolean, onSuccess: () -> Unit) {
        if (!isEditMode || taskId == null) return

        screenModelScope.launch {
            completeTaskUseCase(
                taskId = taskId,
                shouldDelete = shouldDelete
            ).fold(
                onSuccess = {
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.value = TaskDetailUiState.Error(
                        error.message ?: "Ошибка завершения"
                    )
                }
            )
        }
    }

    /**
     * Удаляет текущую задачу
     */
    fun deleteTask(onSuccess: () -> Unit) {
        if (taskId == null || !isEditMode) return

        screenModelScope.launch {
            deleteTaskUseCase(taskId).fold(
                onSuccess = {
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.value = TaskDetailUiState.Error(
                        error.message ?: "Ошибка удаления"
                    )
                }
            )
        }
    }
}

/**
 * UI состояния для экрана детальной информации задачи
 */
sealed class TaskDetailUiState {
    data object Loading : TaskDetailUiState()
    data object Success : TaskDetailUiState()
    data class Error(val message: String) : TaskDetailUiState()
}


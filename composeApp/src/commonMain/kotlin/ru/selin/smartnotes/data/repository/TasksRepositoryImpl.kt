package ru.selin.smartnotes.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.selin.smartnotes.data.mapper.toDomain
import ru.selin.smartnotes.data.mapper.toInsertParams
import ru.selin.smartnotes.data.mapper.toLong
import ru.selin.smartnotes.database.NotesDatabase
import ru.selin.smartnotes.domain.model.Subtask
import ru.selin.smartnotes.domain.model.Task
import ru.selin.smartnotes.domain.repository.TasksRepository
import kotlin.time.Clock

/**
 * TasksRepositoryImpl - Реализация репозитория для работы с задачами и подзадачами
 *
 * Data Layer: Repository Implementation
 *
 * Ключевая бизнес-логика:
 * 1. При изменении подзадачи → автоматически обновляется updatedAt родительской задачи
 * 2. Если все подзадачи выполнены → задача автоматически помечается как isCompleted = true
 * 3. При загрузке задачи автоматически подгружаются её подзадачи
 *
 * @param database Экземпляр NotesDatabase (внедряется через Koin)
 */
class TasksRepositoryImpl(
    private val database: NotesDatabase
) : TasksRepository {

    private val taskQueries = database.taskQueries
    private val subtaskQueries = database.subtaskQueries

    // ========================================
    // ОПЕРАЦИИ С ЗАДАЧАМИ
    // ========================================

    override fun getAllTasks(): Flow<List<Task>> {
        return taskQueries.getAllTasks()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { tasks -> tasks.map { loadTaskWithSubtasks(it.id) } }
    }

    override fun getTasksForToday(): Flow<List<Task>> {
        return taskQueries.getTasksForToday()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { tasks -> tasks.map { loadTaskWithSubtasks(it.id) } }
    }

    override fun getCompletedTasks(): Flow<List<Task>> {
        return taskQueries.getCompletedTasks()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { tasks -> tasks.map { loadTaskWithSubtasks(it.id) } }
    }

    override fun getActiveTasks(): Flow<List<Task>> {
        return taskQueries.getActiveTasks()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { tasks -> tasks.map { loadTaskWithSubtasks(it.id) } }
    }

    override suspend fun getTaskById(id: Long): Task? = withContext(Dispatchers.IO) {
        val taskEntity = taskQueries.getTaskById(id).executeAsOneOrNull() ?: return@withContext null
        val subtasks = loadSubtasksForTask(id)
        taskEntity.toDomain(subtasks)
    }

    override suspend fun insertTask(task: Task): Long = withContext(Dispatchers.IO) {
        val params = task.toInsertParams()
        taskQueries.insertTask(
            title = params.title,
            description = params.description,
            importance = params.importance,
            isToday = params.isToday,
            isCompleted = params.isCompleted,
            createdAt = params.createdAt,
            updatedAt = params.updatedAt
        )

        val taskId = taskQueries.getLastInsertedTask().executeAsOne().id

        // Вставляем подзадачи, если они есть
        task.subtasks.forEach { subtask ->
            val subtaskParams = subtask.copy(taskId = taskId).toInsertParams()
            subtaskQueries.insertSubtask(
                taskId = subtaskParams.taskId,
                title = subtaskParams.title,
                isDone = subtaskParams.isDone
            )
        }

        taskId
    }

    override suspend fun updateTask(task: Task): Unit = withContext(Dispatchers.IO) {
        val params = task.toInsertParams()
        taskQueries.updateTask(
            title = params.title,
            description = params.description,
            importance = params.importance,
            isToday = params.isToday,
            isCompleted = params.isCompleted,
            updatedAt = params.updatedAt,
            id = task.id
        )
    }

    override suspend fun updateTaskTimestamp(taskId: Long, timestamp: Long): Unit =
        withContext(Dispatchers.IO) {
            taskQueries.updateTaskTimestamp(
                updatedAt = timestamp,
                id = taskId
            )
        }

    override suspend fun updateTaskCompletionStatus(
        taskId: Long,
        isCompleted: Boolean,
        timestamp: Long
    ): Unit = withContext(Dispatchers.IO) {
        taskQueries.updateTaskCompletionStatus(
            isCompleted = isCompleted.toLong(),
            updatedAt = timestamp,
            id = taskId
        )
    }

    override suspend fun deleteTask(id: Long): Unit = withContext(Dispatchers.IO) {
        taskQueries.deleteTask(id)
        // Подзадачи удалятся автоматически через CASCADE
    }

    override fun searchTasks(query: String): Flow<List<Task>> {
        return taskQueries.searchTasks(query, query) // Передаём дважды для title и description
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { tasks -> tasks.map { loadTaskWithSubtasks(it.id) } }
    }

    // ========================================
    // ОПЕРАЦИИ С ПОДЗАДАЧАМИ
    // ========================================

    override suspend fun getSubtasksByTaskId(taskId: Long): List<Subtask> =
        withContext(Dispatchers.IO) {
            loadSubtasksForTask(taskId)
        }

    override suspend fun getSubtaskById(id: Long): Subtask? = withContext(Dispatchers.IO) {
        subtaskQueries.getSubtaskById(id)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override suspend fun insertSubtask(subtask: Subtask): Long = withContext(Dispatchers.IO) {
        val params = subtask.toInsertParams()
        subtaskQueries.insertSubtask(
            taskId = params.taskId,
            title = params.title,
            isDone = params.isDone
        )

        // ВАЖНО: Обновляем updatedAt родительской задачи
        updateTaskTimestamp(subtask.taskId, Clock.System.now().epochSeconds)

        subtaskQueries.getLastInsertedSubtask().executeAsOne().id
    }

    override suspend fun updateSubtask(subtask: Subtask): Unit = withContext(Dispatchers.IO) {
        val params = subtask.toInsertParams()
        subtaskQueries.updateSubtask(
            title = params.title,
            isDone = params.isDone,
            id = subtask.id
        )

        // ВАЖНО: Обновляем updatedAt родительской задачи
        val timestamp = Clock.System.now().epochSeconds
        updateTaskTimestamp(subtask.taskId, timestamp)

        // ВАЖНО: Проверяем, все ли подзадачи выполнены
        // Если да → автоматически помечаем задачу как завершённую
        if (areAllSubtasksCompleted(subtask.taskId)) {
            updateTaskCompletionStatus(subtask.taskId, isCompleted = true, timestamp)
        } else {
            // Если была завершена, но теперь не все выполнены → снимаем завершение
            val task = getTaskById(subtask.taskId)
            if (task?.isCompleted == true) {
                updateTaskCompletionStatus(subtask.taskId, isCompleted = false, timestamp)
            }
        }
    }

    override suspend fun toggleSubtaskStatus(subtaskId: Long, isDone: Boolean): Unit =
        withContext(Dispatchers.IO) {
            val subtask = getSubtaskById(subtaskId) ?: return@withContext
            updateSubtask(subtask.copy(isDone = isDone))
        }

    override suspend fun deleteSubtask(id: Long): Unit = withContext(Dispatchers.IO) {
        val subtask = getSubtaskById(id) ?: return@withContext
        val taskId = subtask.taskId

        subtaskQueries.deleteSubtask(id)

        // ВАЖНО: Обновляем updatedAt родительской задачи
        val timestamp = Clock.System.now().epochSeconds
        updateTaskTimestamp(taskId, timestamp)

        // ВАЖНО: Проверяем, все ли оставшиеся подзадачи выполнены
        if (areAllSubtasksCompleted(taskId)) {
            updateTaskCompletionStatus(taskId, isCompleted = true, timestamp)
        }
    }

    override suspend fun areAllSubtasksCompleted(taskId: Long): Boolean =
        withContext(Dispatchers.IO) {
            subtaskQueries.areAllSubtasksCompleted(taskId)
                .executeAsOne()
        }

    // ========================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ========================================

    /**
     * Загружает задачу с её подзадачами
     */
    private suspend fun loadTaskWithSubtasks(taskId: Long): Task {
        val taskEntity = taskQueries.getTaskById(taskId).executeAsOne()
        val subtasks = loadSubtasksForTask(taskId)
        return taskEntity.toDomain(subtasks)
    }

    /**
     * Загружает подзадачи для конкретной задачи
     */
    private suspend fun loadSubtasksForTask(taskId: Long): List<Subtask> {
        return subtaskQueries.getSubtasksByTaskId(taskId)
            .executeAsList()
            .map { it.toDomain() }
    }
}


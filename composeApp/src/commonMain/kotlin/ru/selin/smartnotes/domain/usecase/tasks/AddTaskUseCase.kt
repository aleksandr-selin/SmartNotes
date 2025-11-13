package ru.selin.smartnotes.domain.usecase.tasks

import ru.selin.smartnotes.domain.model.Importance
import ru.selin.smartnotes.domain.model.Subtask
import ru.selin.smartnotes.domain.model.Task
import ru.selin.smartnotes.domain.repository.TasksRepository
import kotlin.time.Clock

/**
 * Use Case для создания новой задачи
 * 
 * Domain Layer: Business Logic
 * 
 * Бизнес-правила:
 * - Автоматически устанавливает createdAt и updatedAt
 * - Валидирует title
 * - Поддерживает создание задачи с подзадачами
 * 
 * @param tasksRepository Репозиторий задач (внедряется через DI)
 */
class AddTaskUseCase(
    private val tasksRepository: TasksRepository
) {
    /**
     * Создаёт новую задачу
     * 
     * @param title Название задачи
     * @param description Описание задачи
     * @param importance Уровень важности (по умолчанию MEDIUM)
     * @param isToday Пометка "на сегодня" (по умолчанию false)
     * @param subtasks Список названий подзадач (опционально)
     * @return Result с ID созданной задачи или ошибкой
     */
    suspend operator fun invoke(
        title: String,
        description: String = "",
        importance: Importance = Importance.MEDIUM,
        isToday: Boolean = false,
        subtasks: List<String> = emptyList()
    ): Result<Long> {
        return try {
            if (title.isBlank()) {
                return Result.failure(IllegalArgumentException("Название задачи не может быть пустым"))
            }
            
            val currentTime = Clock.System.now().epochSeconds
            
            // Создаём подзадачи
            val subtaskList = subtasks
                .filter { it.isNotBlank() }
                .map { subtaskTitle ->
                    Subtask(
                        id = 0,
                        taskId = 0, // Будет установлен при вставке
                        title = subtaskTitle.trim(),
                        isDone = false
                    )
                }
            
            val task = Task(
                id = 0,
                title = title.trim(),
                description = description.trim(),
                importance = importance,
                isToday = isToday,
                isCompleted = false,
                createdAt = currentTime,
                updatedAt = currentTime,
                subtasks = subtaskList
            )
            
            val taskId = tasksRepository.insertTask(task)
            Result.success(taskId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


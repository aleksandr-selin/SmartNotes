package ru.selin.smartnotes.domain.usecase.tasks

import ru.selin.smartnotes.domain.model.Importance
import ru.selin.smartnotes.domain.repository.TasksRepository
import kotlin.time.Clock

/**
 * Use Case для обновления задачи
 *
 * Domain Layer: Business Logic
 *
 * Бизнес-правила:
 * - Автоматически обновляет updatedAt
 * - Валидирует title
 * - Проверяет существование задачи
 * - НЕ изменяет isCompleted (для этого используется CompleteTaskUseCase)
 *
 * @param tasksRepository Репозиторий задач (внедряется через DI)
 */
class UpdateTaskUseCase(
    private val tasksRepository: TasksRepository
) {
    /**
     * Обновляет существующую задачу
     *
     * @param taskId ID задачи
     * @param title Новое название
     * @param description Новое описание
     * @param importance Новый уровень важности
     * @param isToday Новый статус "на сегодня"
     * @return Result с успехом или ошибкой
     */
    suspend operator fun invoke(
        taskId: Long,
        title: String,
        description: String,
        importance: Importance,
        isToday: Boolean
    ): Result<Unit> {
        return try {
            if (title.isBlank()) {
                return Result.failure(IllegalArgumentException("Название задачи не может быть пустым"))
            }

            val existingTask = tasksRepository.getTaskById(taskId)
                ?: return Result.failure(IllegalArgumentException("Задача не найдена"))

            val updatedTask = existingTask.copy(
                title = title.trim(),
                description = description.trim(),
                importance = importance,
                isToday = isToday,
                updatedAt = Clock.System.now().epochSeconds
            )

            tasksRepository.updateTask(updatedTask)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


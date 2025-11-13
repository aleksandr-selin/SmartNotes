package ru.selin.smartnotes.domain.usecase.subtasks

import ru.selin.smartnotes.domain.repository.TasksRepository

/**
 * Use Case для переключения статуса подзадачи
 *
 * Domain Layer: Business Logic
 *
 * - Автоматически обновляет updatedAt родительской задачи
 * - Если все подзадачи выполнены → задача автоматически завершается (isCompleted = true)
 * - Если хотя бы одна подзадача не выполнена → задача становится незавершённой
 *
 * @param tasksRepository Репозиторий задач (внедряется через DI)
 */
class ToggleSubtaskUseCase(
    private val tasksRepository: TasksRepository
) {
    /**
     * Переключает статус выполнения подзадачи
     *
     * @param subtaskId ID подзадачи
     * @param isDone Новый статус (true = выполнено, false = не выполнено)
     * @return Result с успехом или ошибкой
     */
    suspend operator fun invoke(
        subtaskId: Long,
        isDone: Boolean
    ): Result<Unit> {
        return try {
            tasksRepository.toggleSubtaskStatus(subtaskId, isDone)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


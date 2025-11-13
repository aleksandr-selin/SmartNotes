package ru.selin.smartnotes.domain.usecase.subtasks

import ru.selin.smartnotes.domain.repository.TasksRepository

/**
 * Use Case для удаления подзадачи
 * 
 * Domain Layer: Business Logic
 * 
 * ⭐ ВАЖНАЯ ЛОГИКА:
 * - Автоматически обновляет updatedAt родительской задачи
 * - Проверяет, не завершены ли оставшиеся подзадачи
 * 
 * @param tasksRepository Репозиторий задач (внедряется через DI)
 */
class DeleteSubtaskUseCase(
    private val tasksRepository: TasksRepository
) {
    /**
     * Удаляет подзадачу
     * 
     * @param subtaskId ID подзадачи для удаления
     * @return Result с успехом или ошибкой
     */
    suspend operator fun invoke(subtaskId: Long): Result<Unit> {
        return try {
            tasksRepository.deleteSubtask(subtaskId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


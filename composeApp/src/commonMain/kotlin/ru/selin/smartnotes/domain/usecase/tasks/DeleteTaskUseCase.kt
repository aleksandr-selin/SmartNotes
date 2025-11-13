package ru.selin.smartnotes.domain.usecase.tasks

import ru.selin.smartnotes.domain.repository.TasksRepository

/**
 * Use Case для удаления задачи
 * 
 * Domain Layer: Business Logic
 * 
 * Подзадачи удаляются автоматически через каскадное удаление (ON DELETE CASCADE)
 * 
 * @param tasksRepository Репозиторий задач (внедряется через DI)
 */
class DeleteTaskUseCase(
    private val tasksRepository: TasksRepository
) {
    /**
     * Удаляет задачу
     * 
     * @param taskId ID задачи для удаления
     * @return Result с успехом или ошибкой
     */
    suspend operator fun invoke(taskId: Long): Result<Unit> {
        return try {
            tasksRepository.deleteTask(taskId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


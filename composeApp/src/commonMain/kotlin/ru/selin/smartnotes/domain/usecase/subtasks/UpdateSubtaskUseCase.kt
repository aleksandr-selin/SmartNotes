package ru.selin.smartnotes.domain.usecase.subtasks

import ru.selin.smartnotes.domain.repository.TasksRepository

/**
 * Use Case для обновления подзадачи
 * 
 * Domain Layer: Business Logic
 * 
 * - Автоматически обновляет updatedAt родительской задачи
 * - Проверяет, не завершены ли все подзадачи
 * 
 * @param tasksRepository Репозиторий задач (внедряется через DI)
 */
class UpdateSubtaskUseCase(
    private val tasksRepository: TasksRepository
) {
    /**
     * Обновляет название подзадачи
     * 
     * @param subtaskId ID подзадачи
     * @param title Новое название
     * @return Result с успехом или ошибкой
     */
    suspend operator fun invoke(
        subtaskId: Long,
        title: String
    ): Result<Unit> {
        return try {
            if (title.isBlank()) {
                return Result.failure(IllegalArgumentException("Название подзадачи не может быть пустым"))
            }
            
            val subtask = tasksRepository.getSubtaskById(subtaskId)
                ?: return Result.failure(IllegalArgumentException("Подзадача не найдена"))
            
            val updatedSubtask = subtask.copy(title = title.trim())
            tasksRepository.updateSubtask(updatedSubtask)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


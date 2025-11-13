package ru.selin.smartnotes.domain.usecase.subtasks

import ru.selin.smartnotes.domain.model.Subtask
import ru.selin.smartnotes.domain.repository.TasksRepository

/**
 * Use Case для добавления подзадачи
 * 
 * Domain Layer: Business Logic
 * 
 * ⭐ ВАЖНАЯ ЛОГИКА:
 * Автоматически обновляет updatedAt родительской задачи
 * 
 * @param tasksRepository Репозиторий задач (внедряется через DI)
 */
class AddSubtaskUseCase(
    private val tasksRepository: TasksRepository
) {
    /**
     * Добавляет новую подзадачу к задаче
     * 
     * @param taskId ID родительской задачи
     * @param title Название подзадачи
     * @return Result с ID созданной подзадачи или ошибкой
     */
    suspend operator fun invoke(
        taskId: Long,
        title: String
    ): Result<Long> {
        return try {
            if (title.isBlank()) {
                return Result.failure(IllegalArgumentException("Название подзадачи не может быть пустым"))
            }
            
            val subtask = Subtask(
                id = 0,
                taskId = taskId,
                title = title.trim(),
                isDone = false
            )
            
            val subtaskId = tasksRepository.insertSubtask(subtask)
            Result.success(subtaskId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


package ru.selin.smartnotes.domain.usecase.tasks

import ru.selin.smartnotes.domain.model.Task
import ru.selin.smartnotes.domain.repository.TasksRepository

/**
 * Use Case для получения задачи по ID
 * 
 * Domain Layer: Business Logic
 * 
 * Возвращает задачу со всеми подзадачами
 * 
 * @param tasksRepository Репозиторий задач (внедряется через DI)
 */
class GetTaskByIdUseCase(
    private val tasksRepository: TasksRepository
) {
    /**
     * Получает задачу по ID
     * 
     * @param taskId ID задачи
     * @return Задача с подзадачами или null если не найдена
     */
    suspend operator fun invoke(taskId: Long): Task? {
        return tasksRepository.getTaskById(taskId)
    }
}


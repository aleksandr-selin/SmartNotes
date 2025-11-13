package ru.selin.smartnotes.domain.usecase.tasks

import kotlinx.coroutines.flow.Flow
import ru.selin.smartnotes.domain.model.Task
import ru.selin.smartnotes.domain.repository.TasksRepository

/**
 * Use Case для получения задач "на сегодня"
 * 
 * Domain Layer: Business Logic
 * 
 * Возвращает только задачи с пометкой isToday = true
 * 
 * @param tasksRepository Репозиторий задач (внедряется через DI)
 */
class GetTasksForTodayUseCase(
    private val tasksRepository: TasksRepository
) {
    /**
     * Получает задачи "на сегодня"
     * 
     * @return Flow со списком задач для сегодняшнего дня
     */
    operator fun invoke(): Flow<List<Task>> {
        return tasksRepository.getTasksForToday()
    }
}


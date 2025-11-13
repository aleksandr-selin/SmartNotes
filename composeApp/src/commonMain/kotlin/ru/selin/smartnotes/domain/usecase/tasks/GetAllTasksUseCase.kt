package ru.selin.smartnotes.domain.usecase.tasks

import kotlinx.coroutines.flow.Flow
import ru.selin.smartnotes.domain.model.Task
import ru.selin.smartnotes.domain.repository.TasksRepository

/**
 * Use Case для получения задач с фильтрацией
 * 
 * Domain Layer: Business Logic
 * 
 * Поддерживает различные фильтры для отображения задач
 * 
 * @param tasksRepository Репозиторий задач (внедряется через DI)
 */
class GetAllTasksUseCase(
    private val tasksRepository: TasksRepository
) {
    /**
     * Получает задачи с применением фильтра
     * 
     * @param filter Фильтр для задач (по умолчанию ALL)
     * @return Flow со списком задач
     */
    operator fun invoke(filter: TaskFilter = TaskFilter.ALL): Flow<List<Task>> {
        return when (filter) {
            TaskFilter.ALL -> tasksRepository.getAllTasks()
            TaskFilter.TODAY -> tasksRepository.getTasksForToday()
            TaskFilter.ACTIVE -> tasksRepository.getActiveTasks()
            TaskFilter.COMPLETED -> tasksRepository.getCompletedTasks()
        }
    }
}

/**
 * Фильтры для задач
 */
enum class TaskFilter {
    ALL,        // Все задачи
    TODAY,      // Задачи "на сегодня"
    ACTIVE,     // Активные (незавершённые)
    COMPLETED   // Завершённые
}


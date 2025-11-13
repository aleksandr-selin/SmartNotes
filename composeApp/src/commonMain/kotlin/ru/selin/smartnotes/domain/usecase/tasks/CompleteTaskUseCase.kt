package ru.selin.smartnotes.domain.usecase.tasks

import ru.selin.smartnotes.domain.repository.TasksRepository
import kotlin.time.Clock

/**
 * Use Case для завершения задачи
 * 
 * Domain Layer: Business Logic
 * 
 * При завершении задачи пользователь выбирает:
 * 1. УДАЛИТЬ задачу → задача и подзадачи удаляются каскадно
 * 2. ОСТАВИТЬ задачу → isCompleted = true, updatedAt фиксируется как время завершения
 * 
 * @param tasksRepository Репозиторий задач (внедряется через DI)
 */
class CompleteTaskUseCase(
    private val tasksRepository: TasksRepository
) {
    /**
     * Завершает задачу согласно выбору пользователя
     * 
     * @param taskId ID задачи
     * @param shouldDelete true = удалить задачу, false = оставить как завершённую
     * @return Result с успехом или ошибкой
     */
    suspend operator fun invoke(
        taskId: Long,
        shouldDelete: Boolean
    ): Result<Unit> {
        return try {
            if (shouldDelete) {
                // Вариант 1: Удаляем задачу (подзадачи удалятся каскадно через ON DELETE CASCADE)
                tasksRepository.deleteTask(taskId)
            } else {
                // Вариант 2: Помечаем как завершённую с фиксацией времени
                tasksRepository.updateTaskCompletionStatus(
                    taskId = taskId,
                    isCompleted = true,
                    timestamp = Clock.System.now().epochSeconds
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


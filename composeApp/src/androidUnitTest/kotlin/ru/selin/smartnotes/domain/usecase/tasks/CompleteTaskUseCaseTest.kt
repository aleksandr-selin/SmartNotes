package ru.selin.smartnotes.domain.usecase.tasks

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import ru.selin.smartnotes.domain.repository.TasksRepository
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Тесты для CompleteTaskUseCase
 *
 * Покрытие:
 * - shouldDelete = true → вызывается deleteTask
 * - shouldDelete = false → вызывается updateTaskCompletionStatus
 * - Обработка ошибок
 */
class CompleteTaskUseCaseTest {

    @Test
    fun `complete task with shouldDelete true calls deleteTask`() = runTest {
        // Given
        val repository = mockk<TasksRepository>(relaxed = true)
        val useCase = CompleteTaskUseCase(repository)

        // When
        val result = useCase(taskId = 1L, shouldDelete = true)

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.deleteTask(1L) }
        coVerify(exactly = 0) { repository.updateTaskCompletionStatus(any(), any(), any()) }
    }

    @Test
    fun `complete task with shouldDelete false calls updateTaskCompletionStatus`() = runTest {
        // Given
        val repository = mockk<TasksRepository>(relaxed = true)
        val useCase = CompleteTaskUseCase(repository)

        // When
        val result = useCase(taskId = 1L, shouldDelete = false)

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { repository.deleteTask(any()) }
        coVerify(exactly = 1) {
            repository.updateTaskCompletionStatus(
                taskId = 1L,
                isCompleted = true,
                timestamp = any()
            )
        }
    }

    @Test
    fun `complete task with multiple different tasks uses correct taskId`() = runTest {
        // Given
        val repository = mockk<TasksRepository>(relaxed = true)
        val useCase = CompleteTaskUseCase(repository)

        // When
        useCase(taskId = 42L, shouldDelete = true)
        useCase(taskId = 100L, shouldDelete = false)

        // Then
        coVerify(exactly = 1) { repository.deleteTask(42L) }
        coVerify(exactly = 1) {
            repository.updateTaskCompletionStatus(100L, true, any())
        }
    }

    @Test
    fun `repository exception is wrapped in Result failure`() = runTest {
        // Given
        val repository = mockk<TasksRepository>()
        val exception = RuntimeException("Database error")
        coEvery { repository.deleteTask(any()) } throws exception
        val useCase = CompleteTaskUseCase(repository)

        // When
        val result = useCase(taskId = 1L, shouldDelete = true)

        // Then
        assertTrue(result.isFailure)
    }
}


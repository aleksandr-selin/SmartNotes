package ru.selin.smartnotes.domain.usecase.tasks

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import ru.selin.smartnotes.domain.model.Importance
import ru.selin.smartnotes.domain.repository.TasksRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Тесты для AddTaskUseCase
 * 
 * Покрытие:
 * - Валидация пустого title
 * - Успешное создание задачи
 * - Создание задачи с подзадачами
 * - Фильтрация пустых подзадач
 */
class AddTaskUseCaseTest {
    
    @Test
    fun `adding task with blank title returns failure`() = runTest {
        // Given
        val repository = mockk<TasksRepository>()
        val useCase = AddTaskUseCase(repository)
        
        // When
        val result = useCase(title = "", description = "Description")
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(
            "Название задачи не может быть пустым",
            result.exceptionOrNull()?.message
        )
        coVerify(exactly = 0) { repository.insertTask(any()) }
    }
    
    @Test
    fun `adding task with valid data returns success with id`() = runTest {
        // Given
        val repository = mockk<TasksRepository>()
        coEvery { repository.insertTask(any()) } returns 100L
        val useCase = AddTaskUseCase(repository)
        
        // When
        val result = useCase(
            title = "Test Task",
            description = "Description",
            importance = Importance.HIGH,
            isToday = true
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(100L, result.getOrNull())
        coVerify(exactly = 1) { repository.insertTask(any()) }
    }
    
    @Test
    fun `adding task with subtasks creates task with subtasks`() = runTest {
        // Given
        val repository = mockk<TasksRepository>()
        coEvery { repository.insertTask(any()) } returns 1L
        val useCase = AddTaskUseCase(repository)
        
        // When
        useCase(
            title = "Task",
            description = "Desc",
            subtasks = listOf("Subtask 1", "Subtask 2")
        )
        
        // Then
        coVerify(exactly = 1) {
            repository.insertTask(
                match { task ->
                    task.subtasks.size == 2 &&
                    task.subtasks[0].title == "Subtask 1" &&
                    task.subtasks[1].title == "Subtask 2"
                }
            )
        }
    }
    
    @Test
    fun `adding task filters out blank subtasks`() = runTest {
        // Given
        val repository = mockk<TasksRepository>()
        coEvery { repository.insertTask(any()) } returns 1L
        val useCase = AddTaskUseCase(repository)
        
        // When
        useCase(
            title = "Task",
            description = "Desc",
            subtasks = listOf("Subtask 1", "", "  ", "Subtask 2")
        )
        
        // Then
        coVerify(exactly = 1) {
            repository.insertTask(
                match { task ->
                    task.subtasks.size == 2
                }
            )
        }
    }
    
    @Test
    fun `adding task with default parameters uses correct defaults`() = runTest {
        // Given
        val repository = mockk<TasksRepository>()
        coEvery { repository.insertTask(any()) } returns 1L
        val useCase = AddTaskUseCase(repository)
        
        // When
        useCase(title = "Task")
        
        // Then
        coVerify(exactly = 1) {
            repository.insertTask(
                match { task ->
                    task.importance == Importance.MEDIUM &&
                    task.isToday == false &&
                    task.isCompleted == false &&
                    task.description == "" &&
                    task.subtasks.isEmpty()
                }
            )
        }
    }
}


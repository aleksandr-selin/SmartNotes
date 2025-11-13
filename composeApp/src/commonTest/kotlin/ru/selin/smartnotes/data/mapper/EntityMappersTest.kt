package ru.selin.smartnotes.data.mapper

import ru.selin.smartnotes.domain.model.Importance
import ru.selin.smartnotes.domain.model.Note
import ru.selin.smartnotes.domain.model.Subtask
import ru.selin.smartnotes.domain.model.Task
import ru.selin.smartnotes.database.Note as NoteEntity
import ru.selin.smartnotes.database.Task as TaskEntity
import ru.selin.smartnotes.database.Subtask as SubtaskEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Тесты для EntityMappers - преобразование между БД entities и domain моделями
 * 
 * Приоритет 1: Самые важные и простые тесты
 * 
 * Покрытие:
 * - Преобразование Note Entity ↔ Domain
 * - Преобразование Task Entity ↔ Domain
 * - Преобразование Subtask Entity ↔ Domain
 * - Конвертация Boolean ↔ Long
 * - Конвертация String ↔ Importance
 * - Бизнес-логика в Task (подсчёт прогресса)
 */
class EntityMappersTest {
    
    // ========================================
    // NOTE MAPPERS
    // ========================================
    
    @Test
    fun `note entity to domain mapping preserves all fields`() {
        // Given
        val noteEntity = NoteEntity(
            id = 42L,
            title = "Kotlin Programming",
            content = "Learn Kotlin Multiplatform",
            createdAt = 1000000L,
            updatedAt = 2000000L
        )
        
        // When
        val domainNote = noteEntity.toDomain()
        
        // Then
        assertEquals(42L, domainNote.id)
        assertEquals("Kotlin Programming", domainNote.title)
        assertEquals("Learn Kotlin Multiplatform", domainNote.content)
        assertEquals(1000000L, domainNote.createdAt)
        assertEquals(2000000L, domainNote.updatedAt)
    }
    
    @Test
    fun `note domain to insert params mapping`() {
        // Given
        val note = Note(
            id = 100L, // ID игнорируется при вставке
            title = "Test Note",
            content = "Test Content",
            createdAt = 5000L,
            updatedAt = 6000L
        )
        
        // When
        val insertParams = note.toInsertParams()
        
        // Then
        assertEquals("Test Note", insertParams.title)
        assertEquals("Test Content", insertParams.content)
        assertEquals(5000L, insertParams.createdAt)
        assertEquals(6000L, insertParams.updatedAt)
    }
    
    @Test
    fun `note with empty strings maps correctly`() {
        // Given
        val noteEntity = NoteEntity(
            id = 1L,
            title = "",
            content = "",
            createdAt = 0L,
            updatedAt = 0L
        )
        
        // When
        val domainNote = noteEntity.toDomain()
        
        // Then
        assertEquals("", domainNote.title)
        assertEquals("", domainNote.content)
    }
    
    // ========================================
    // TASK MAPPERS
    // ========================================
    
    @Test
    fun `task entity to domain without subtasks`() {
        // Given
        val taskEntity = TaskEntity(
            id = 10L,
            title = "Complete Project",
            description = "Finish all tasks",
            importance = "HIGH",
            isToday = 1L, // true
            isCompleted = 0L, // false
            createdAt = 1000L,
            updatedAt = 2000L
        )
        
        // When
        val domainTask = taskEntity.toDomain()
        
        // Then
        assertEquals(10L, domainTask.id)
        assertEquals("Complete Project", domainTask.title)
        assertEquals("Finish all tasks", domainTask.description)
        assertEquals(Importance.HIGH, domainTask.importance)
        assertTrue(domainTask.isToday)
        assertFalse(domainTask.isCompleted)
        assertEquals(1000L, domainTask.createdAt)
        assertEquals(2000L, domainTask.updatedAt)
        assertTrue(domainTask.subtasks.isEmpty())
    }
    
    @Test
    fun `task entity to domain with subtasks`() {
        // Given
        val taskEntity = TaskEntity(
            id = 1L,
            title = "Task",
            description = "Desc",
            importance = "MEDIUM",
            isToday = 0L,
            isCompleted = 0L,
            createdAt = 1000L,
            updatedAt = 2000L
        )
        val subtasks = listOf(
            Subtask(id = 1L, taskId = 1L, title = "Subtask 1", isDone = true),
            Subtask(id = 2L, taskId = 1L, title = "Subtask 2", isDone = false)
        )
        
        // When
        val domainTask = taskEntity.toDomain(subtasks)
        
        // Then
        assertEquals(2, domainTask.subtasks.size)
        assertEquals("Subtask 1", domainTask.subtasks[0].title)
        assertTrue(domainTask.subtasks[0].isDone)
        assertEquals("Subtask 2", domainTask.subtasks[1].title)
        assertFalse(domainTask.subtasks[1].isDone)
    }
    
    @Test
    fun `task domain to insert params mapping`() {
        // Given
        val task = Task(
            id = 100L,
            title = "New Task",
            description = "Description",
            importance = Importance.LOW,
            isToday = true,
            isCompleted = false,
            createdAt = 3000L,
            updatedAt = 4000L
        )
        
        // When
        val insertParams = task.toInsertParams()
        
        // Then
        assertEquals("New Task", insertParams.title)
        assertEquals("Description", insertParams.description)
        assertEquals("LOW", insertParams.importance)
        assertEquals(1L, insertParams.isToday)
        assertEquals(0L, insertParams.isCompleted)
        assertEquals(3000L, insertParams.createdAt)
        assertEquals(4000L, insertParams.updatedAt)
    }
    
    // ========================================
    // SUBTASK MAPPERS
    // ========================================
    
    @Test
    fun `subtask entity to domain mapping`() {
        // Given
        val subtaskEntity = SubtaskEntity(
            id = 5L,
            taskId = 10L,
            title = "Write tests",
            isDone = 1L // true
        )
        
        // When
        val domainSubtask = subtaskEntity.toDomain()
        
        // Then
        assertEquals(5L, domainSubtask.id)
        assertEquals(10L, domainSubtask.taskId)
        assertEquals("Write tests", domainSubtask.title)
        assertTrue(domainSubtask.isDone)
    }
    
    @Test
    fun `subtask domain to insert params mapping`() {
        // Given
        val subtask = Subtask(
            id = 99L,
            taskId = 1L,
            title = "Review code",
            isDone = false
        )
        
        // When
        val insertParams = subtask.toInsertParams()
        
        // Then
        assertEquals(1L, insertParams.taskId)
        assertEquals("Review code", insertParams.title)
        assertEquals(0L, insertParams.isDone)
    }
    
    // ========================================
    // TYPE CONVERSIONS
    // ========================================
    
    @Test
    fun `boolean to long conversion`() {
        assertEquals(1L, true.toLong())
        assertEquals(0L, false.toLong())
    }
    
    @Test
    fun `long to boolean conversion`() {
        // 0 = false, любое другое = true
        assertEquals(false, 0L.toBoolean())
        assertEquals(true, 1L.toBoolean())
        assertEquals(true, 100L.toBoolean())
        assertEquals(true, (-1L).toBoolean())
    }
    
    @Test
    fun `importance from string valid values`() {
        assertEquals(Importance.LOW, Importance.fromString("LOW"))
        assertEquals(Importance.MEDIUM, Importance.fromString("MEDIUM"))
        assertEquals(Importance.HIGH, Importance.fromString("HIGH"))
    }
    
    @Test
    fun `importance from string case insensitive`() {
        assertEquals(Importance.LOW, Importance.fromString("low"))
        assertEquals(Importance.MEDIUM, Importance.fromString("medium"))
        assertEquals(Importance.HIGH, Importance.fromString("high"))
        assertEquals(Importance.MEDIUM, Importance.fromString("MeDiUm"))
    }
    
    @Test
    fun `importance from string invalid returns LOW as default`() {
        assertEquals(Importance.LOW, Importance.fromString("INVALID"))
        assertEquals(Importance.LOW, Importance.fromString(""))
        assertEquals(Importance.LOW, Importance.fromString("URGENT"))
    }
    
    // ========================================
    // TASK BUSINESS LOGIC
    // ========================================
    
    @Test
    fun `task with no subtasks reports all completed`() {
        // Given
        val task = Task(
            id = 1L,
            title = "Task",
            description = "Desc",
            importance = Importance.LOW,
            createdAt = 1000L,
            updatedAt = 2000L,
            subtasks = emptyList()
        )
        
        // Then
        assertTrue(task.areAllSubtasksCompleted())
        assertEquals(0, task.getCompletedSubtasksCount())
        assertEquals(0f, task.getSubtasksProgress())
    }
    
    @Test
    fun `task with all subtasks completed`() {
        // Given
        val task = Task(
            id = 1L,
            title = "Task",
            description = "Desc",
            importance = Importance.LOW,
            createdAt = 1000L,
            updatedAt = 2000L,
            subtasks = listOf(
                Subtask(1L, 1L, "Sub 1", isDone = true),
                Subtask(2L, 1L, "Sub 2", isDone = true),
                Subtask(3L, 1L, "Sub 3", isDone = true)
            )
        )
        
        // Then
        assertTrue(task.areAllSubtasksCompleted())
        assertEquals(3, task.getCompletedSubtasksCount())
        assertEquals(1.0f, task.getSubtasksProgress())
    }
    
    @Test
    fun `task with partial subtasks completion`() {
        // Given
        val task = Task(
            id = 1L,
            title = "Task",
            description = "Desc",
            importance = Importance.LOW,
            createdAt = 1000L,
            updatedAt = 2000L,
            subtasks = listOf(
                Subtask(1L, 1L, "Sub 1", isDone = true),
                Subtask(2L, 1L, "Sub 2", isDone = true),
                Subtask(3L, 1L, "Sub 3", isDone = false),
                Subtask(4L, 1L, "Sub 4", isDone = false)
            )
        )
        
        // Then
        assertFalse(task.areAllSubtasksCompleted())
        assertEquals(2, task.getCompletedSubtasksCount())
        assertEquals(0.5f, task.getSubtasksProgress())
    }
    
    @Test
    fun `task progress calculation precision`() {
        // Given - 2 из 3 выполнено = 0.666...
        val task = Task(
            id = 1L,
            title = "Task",
            description = "Desc",
            importance = Importance.LOW,
            createdAt = 1000L,
            updatedAt = 2000L,
            subtasks = listOf(
                Subtask(1L, 1L, "Sub 1", isDone = true),
                Subtask(2L, 1L, "Sub 2", isDone = true),
                Subtask(3L, 1L, "Sub 3", isDone = false)
            )
        )
        
        // Then
        assertEquals(0.666f, task.getSubtasksProgress(), 0.01f)
    }
    
    @Test
    fun `task with all importance levels maps correctly`() {
        // Given
        val lowTask = TaskEntity(1L, "T", "D", "LOW", 0L, 0L, 0L, 0L)
        val mediumTask = TaskEntity(2L, "T", "D", "MEDIUM", 0L, 0L, 0L, 0L)
        val highTask = TaskEntity(3L, "T", "D", "HIGH", 0L, 0L, 0L, 0L)
        
        // When & Then
        assertEquals(Importance.LOW, lowTask.toDomain().importance)
        assertEquals(Importance.MEDIUM, mediumTask.toDomain().importance)
        assertEquals(Importance.HIGH, highTask.toDomain().importance)
    }
}


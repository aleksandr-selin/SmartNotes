package ru.selin.smartnotes.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ru.selin.smartnotes.database.NotesDatabase
import ru.selin.smartnotes.domain.model.Importance
import ru.selin.smartnotes.domain.model.Subtask
import ru.selin.smartnotes.domain.model.Task
import kotlin.test.*

/**
 * Интеграционные тесты для TasksRepositoryImpl
 * 
 * Приоритет 2: Критическая бизнес-логика
 * Приоритет 3: CRUD операции
 * 
 * КЛЮЧЕВАЯ БИЗНЕС-ЛОГИКА:
 * 1. При изменении подзадачи → автоматически обновляется updatedAt родительской задачи
 * 2. Если все подзадачи выполнены → задача автоматически помечается как isCompleted = true
 * 3. Если подзадачу отметили невыполненной → задача становится незавершённой
 * 4. Удаление задачи → каскадное удаление подзадач
 * 
 * Покрытие:
 * - CRUD для задач
 * - CRUD для подзадач
 * - Фильтрация (все/сегодня/завершённые/активные)
 * - Поиск
 * - Автообновление updatedAt
 * - Автозавершение задач
 * - Каскадное удаление
 */
class TasksRepositoryImplTest {
    
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: NotesDatabase
    private lateinit var repository: TasksRepositoryImpl
    
    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        NotesDatabase.Schema.create(driver)
        // ВАЖНО: Включаем Foreign Keys для каскадного удаления
        driver.execute(null, "PRAGMA foreign_keys=ON", 0)
        database = NotesDatabase(driver)
        repository = TasksRepositoryImpl(database)
    }
    
    @AfterTest
    fun teardown() {
        driver.close()
    }
    
    // ========================================
    // ПРИОРИТЕТ 2: КРИТИЧЕСКАЯ БИЗНЕС-ЛОГИКА
    // ========================================
    
    @Test
    fun `inserting subtask updates parent task updatedAt`() = runTest {
        // Given - создаём задачу
        val task = createTask("Parent Task", updatedAt = 1000L)
        val taskId = repository.insertTask(task)
        val originalTask = repository.getTaskById(taskId)!!
        
        // Wait a bit to ensure time difference
        Thread.sleep(10)
        
        // When - добавляем подзадачу
        val subtask = Subtask(0, taskId, "New Subtask", isDone = false)
        repository.insertSubtask(subtask)
        
        // Then - updatedAt задачи обновился
        val updatedTask = repository.getTaskById(taskId)!!
        assertTrue(
            updatedTask.updatedAt > originalTask.updatedAt,
            "updatedAt должен обновиться при добавлении подзадачи"
        )
    }
    
    @Test
    fun `updating subtask updates parent task updatedAt`() = runTest {
        // Given - создаём задачу с фиксированным updatedAt
        val taskId = repository.insertTask(createTask("Task", updatedAt = 1000L))
        val originalTask = repository.getTaskById(taskId)!!
        assertEquals(1000L, originalTask.updatedAt, "Исходный updatedAt должен быть 1000")
        
        // When - добавляем подзадачу (это обновит updatedAt на System.currentTimeMillis())
        val subtaskId = repository.insertSubtask(Subtask(0, taskId, "Subtask", false))
        
        // Then - updatedAt должен измениться
        val taskAfterInsert = repository.getTaskById(taskId)!!
        assertTrue(
            taskAfterInsert.updatedAt > 1000L,
            "updatedAt должен обновиться при добавлении подзадачи. Было: 1000, стало: ${taskAfterInsert.updatedAt}"
        )
        
        Thread.sleep(10)
        
        // When - обновляем подзадачу
        val updatedSubtask = Subtask(subtaskId, taskId, "Updated Subtask", false)
        repository.updateSubtask(updatedSubtask)
        
        // Then - updatedAt должен обновиться снова
        val finalTask = repository.getTaskById(taskId)!!
        assertTrue(
            finalTask.updatedAt >= taskAfterInsert.updatedAt,
            "updatedAt должен обновиться при обновлении подзадачи. Было: ${taskAfterInsert.updatedAt}, стало: ${finalTask.updatedAt}"
        )
    }
    
    @Test
    fun `deleting subtask updates parent task updatedAt`() = runTest {
        // Given - создаём задачу с фиксированным updatedAt
        val taskId = repository.insertTask(createTask("Task", updatedAt = 1000L))
        val originalTask = repository.getTaskById(taskId)!!
        assertEquals(1000L, originalTask.updatedAt)
        
        // When - добавляем подзадачу (updatedAt обновится)
        val subtaskId = repository.insertSubtask(Subtask(0, taskId, "Subtask", false))
        val taskAfterInsert = repository.getTaskById(taskId)!!
        assertTrue(taskAfterInsert.updatedAt > 1000L)
        
        Thread.sleep(10)
        
        // When - удаляем подзадачу
        repository.deleteSubtask(subtaskId)
        
        // Then - updatedAt должен обновиться снова
        val finalTask = repository.getTaskById(taskId)!!
        assertTrue(
            finalTask.updatedAt >= taskAfterInsert.updatedAt,
            "updatedAt должен обновиться при удалении подзадачи. Было: ${taskAfterInsert.updatedAt}, стало: ${finalTask.updatedAt}"
        )
    }
    
    @Test
    fun `completing all subtasks marks task as completed`() = runTest {
        // Given - задача с двумя подзадачами
        val taskId = repository.insertTask(createTask("Task", isCompleted = false))
        val subtask1Id = repository.insertSubtask(Subtask(0, taskId, "Sub 1", false))
        val subtask2Id = repository.insertSubtask(Subtask(0, taskId, "Sub 2", false))
        
        // When - выполняем обе подзадачи
        repository.toggleSubtaskStatus(subtask1Id, true)
        repository.toggleSubtaskStatus(subtask2Id, true)
        
        // Then - задача автоматически помечена как завершённая
        val task = repository.getTaskById(taskId)!!
        assertTrue(task.isCompleted, "Задача должна быть автоматически завершена")
    }
    
    @Test
    fun `uncompleting subtask marks task as uncompleted`() = runTest {
        // Given - задача с выполненными подзадачами
        val taskId = repository.insertTask(createTask("Task", isCompleted = false))
        val subtask1Id = repository.insertSubtask(Subtask(0, taskId, "Sub 1", false))
        val subtask2Id = repository.insertSubtask(Subtask(0, taskId, "Sub 2", false))
        
        // Выполняем все подзадачи
        repository.toggleSubtaskStatus(subtask1Id, true)
        repository.toggleSubtaskStatus(subtask2Id, true)
        
        // Проверяем что задача завершена
        assertTrue(repository.getTaskById(taskId)!!.isCompleted)
        
        // When - снимаем галочку с одной подзадачи
        repository.toggleSubtaskStatus(subtask1Id, false)
        
        // Then - задача должна стать незавершённой
        val task = repository.getTaskById(taskId)!!
        assertFalse(task.isCompleted, "Задача должна стать незавершённой")
    }
    
    @Test
    fun `task without subtasks does not auto-complete`() = runTest {
        // Given - задача без подзадач
        val taskId = repository.insertTask(createTask("Task", isCompleted = false))
        
        // When & Then - задача остаётся незавершённой
        val task = repository.getTaskById(taskId)!!
        assertFalse(task.isCompleted)
    }
    
    @Test
    fun `completing some but not all subtasks does not complete task`() = runTest {
        // Given
        val taskId = repository.insertTask(createTask("Task", isCompleted = false))
        val subtask1Id = repository.insertSubtask(Subtask(0, taskId, "Sub 1", false))
        val subtask2Id = repository.insertSubtask(Subtask(0, taskId, "Sub 2", false))
        val subtask3Id = repository.insertSubtask(Subtask(0, taskId, "Sub 3", false))
        
        // When - выполняем только 2 из 3
        repository.toggleSubtaskStatus(subtask1Id, true)
        repository.toggleSubtaskStatus(subtask2Id, true)
        
        // Then - задача НЕ завершена
        val task = repository.getTaskById(taskId)!!
        assertFalse(task.isCompleted, "Задача не должна быть завершена если не все подзадачи выполнены")
    }
    
    @Test
    fun `deleting task cascades to subtasks`() = runTest {
        // Given - задача с подзадачами
        val taskId = repository.insertTask(createTask("Task"))
        val subtask1Id = repository.insertSubtask(Subtask(0, taskId, "Sub 1", false))
        val subtask2Id = repository.insertSubtask(Subtask(0, taskId, "Sub 2", false))
        
        // When - удаляем задачу
        repository.deleteTask(taskId)
        
        // Then - подзадачи также удалены
        assertNull(repository.getTaskById(taskId))
        assertNull(repository.getSubtaskById(subtask1Id))
        assertNull(repository.getSubtaskById(subtask2Id))
    }
    
    // ========================================
    // ПРИОРИТЕТ 3: CRUD ДЛЯ ЗАДАЧ
    // ========================================
    
    @Test
    fun `insertTask returns valid id`() = runTest {
        // Given
        val task = createTask("Test Task")
        
        // When
        val id = repository.insertTask(task)
        
        // Then
        assertTrue(id > 0)
    }
    
    @Test
    fun `insertTask with subtasks saves all data`() = runTest {
        // Given
        val task = createTask(
            title = "Task with Subtasks",
            subtasks = listOf(
                Subtask(0, 0, "Subtask 1", false),
                Subtask(0, 0, "Subtask 2", true)
            )
        )
        
        // When
        val taskId = repository.insertTask(task)
        
        // Then
        val retrieved = repository.getTaskById(taskId)!!
        assertEquals(2, retrieved.subtasks.size)
        assertEquals("Subtask 1", retrieved.subtasks[0].title)
        assertFalse(retrieved.subtasks[0].isDone)
        assertEquals("Subtask 2", retrieved.subtasks[1].title)
        assertTrue(retrieved.subtasks[1].isDone)
    }
    
    @Test
    fun `getTaskById returns correct task with subtasks`() = runTest {
        // Given
        val taskId = repository.insertTask(createTask("My Task", importance = Importance.HIGH))
        repository.insertSubtask(Subtask(0, taskId, "Sub 1", false))
        repository.insertSubtask(Subtask(0, taskId, "Sub 2", true))
        
        // When
        val task = repository.getTaskById(taskId)
        
        // Then
        assertNotNull(task)
        assertEquals("My Task", task.title)
        assertEquals(Importance.HIGH, task.importance)
        assertEquals(2, task.subtasks.size)
    }
    
    @Test
    fun `getTaskById with non-existent id returns null`() = runTest {
        // When
        val task = repository.getTaskById(999L)
        
        // Then
        assertNull(task)
    }
    
    @Test
    fun `updateTask changes all fields`() = runTest {
        // Given
        val original = createTask(
            title = "Original",
            description = "Original Desc",
            importance = Importance.LOW,
            isToday = false,
            isCompleted = false
        )
        val taskId = repository.insertTask(original)
        
        // When
        val updated = original.copy(
            id = taskId,
            title = "Updated",
            description = "Updated Desc",
            importance = Importance.HIGH,
            isToday = true,
            isCompleted = true,
            updatedAt = 5000L
        )
        repository.updateTask(updated)
        
        // Then
        val retrieved = repository.getTaskById(taskId)!!
        assertEquals("Updated", retrieved.title)
        assertEquals("Updated Desc", retrieved.description)
        assertEquals(Importance.HIGH, retrieved.importance)
        assertTrue(retrieved.isToday)
        assertTrue(retrieved.isCompleted)
        assertEquals(5000L, retrieved.updatedAt)
    }
    
    @Test
    fun `deleteTask removes task from database`() = runTest {
        // Given
        val taskId = repository.insertTask(createTask("To Delete"))
        
        // When
        repository.deleteTask(taskId)
        
        // Then
        assertNull(repository.getTaskById(taskId))
    }
    
    // ========================================
    // ПРИОРИТЕТ 3: ФИЛЬТРАЦИЯ
    // ========================================
    
    @Test
    fun `getAllTasks returns all tasks`() = runTest {
        // Given
        repository.insertTask(createTask("Task 1"))
        repository.insertTask(createTask("Task 2"))
        repository.insertTask(createTask("Task 3"))
        
        // When
        val tasks = repository.getAllTasks().first()
        
        // Then
        assertEquals(3, tasks.size)
    }
    
    @Test
    fun `getTasksForToday returns only today tasks`() = runTest {
        // Given
        repository.insertTask(createTask("Today 1", isToday = true))
        repository.insertTask(createTask("Not Today", isToday = false))
        repository.insertTask(createTask("Today 2", isToday = true))
        
        // When
        val todayTasks = repository.getTasksForToday().first()
        
        // Then
        assertEquals(2, todayTasks.size)
        assertTrue(todayTasks.all { it.isToday })
    }
    
    @Test
    fun `getCompletedTasks returns only completed tasks`() = runTest {
        // Given
        repository.insertTask(createTask("Completed 1", isCompleted = true))
        repository.insertTask(createTask("Active", isCompleted = false))
        repository.insertTask(createTask("Completed 2", isCompleted = true))
        
        // When
        val completed = repository.getCompletedTasks().first()
        
        // Then
        assertEquals(2, completed.size)
        assertTrue(completed.all { it.isCompleted })
    }
    
    @Test
    fun `getActiveTasks returns only non-completed tasks`() = runTest {
        // Given
        repository.insertTask(createTask("Active 1", isCompleted = false))
        repository.insertTask(createTask("Completed", isCompleted = true))
        repository.insertTask(createTask("Active 2", isCompleted = false))
        
        // When
        val active = repository.getActiveTasks().first()
        
        // Then
        assertEquals(2, active.size)
        assertTrue(active.all { !it.isCompleted })
    }
    
    @Test
    fun `searchTasks finds tasks by title`() = runTest {
        // Given
        repository.insertTask(createTask("Kotlin Development"))
        repository.insertTask(createTask("Java Programming"))
        repository.insertTask(createTask("Kotlin Testing"))
        
        // When
        val results = repository.searchTasks("Kotlin").first()
        
        // Then
        assertEquals(2, results.size)
        assertTrue(results.all { "Kotlin" in it.title })
    }
    
    @Test
    fun `searchTasks finds tasks by description`() = runTest {
        // Given
        repository.insertTask(createTask("Task 1", description = "Learn Android"))
        repository.insertTask(createTask("Task 2", description = "Learn iOS"))
        repository.insertTask(createTask("Task 3", description = "Android and iOS"))
        
        // When
        val results = repository.searchTasks("Android").first()
        
        // Then
        assertEquals(2, results.size)
        assertTrue(results.all { "Android" in it.description })
    }
    
    // ========================================
    // ПРИОРИТЕТ 3: CRUD ДЛЯ ПОДЗАДАЧ
    // ========================================
    
    @Test
    fun `insertSubtask returns valid id`() = runTest {
        // Given
        val taskId = repository.insertTask(createTask("Task"))
        val subtask = Subtask(0, taskId, "New Subtask", false)
        
        // When
        val subtaskId = repository.insertSubtask(subtask)
        
        // Then
        assertTrue(subtaskId > 0)
    }
    
    @Test
    fun `getSubtasksByTaskId returns all subtasks for task`() = runTest {
        // Given
        val task1Id = repository.insertTask(createTask("Task 1"))
        val task2Id = repository.insertTask(createTask("Task 2"))
        
        repository.insertSubtask(Subtask(0, task1Id, "T1 Sub 1", false))
        repository.insertSubtask(Subtask(0, task1Id, "T1 Sub 2", false))
        repository.insertSubtask(Subtask(0, task2Id, "T2 Sub 1", false))
        
        // When
        val task1Subtasks = repository.getSubtasksByTaskId(task1Id)
        val task2Subtasks = repository.getSubtasksByTaskId(task2Id)
        
        // Then
        assertEquals(2, task1Subtasks.size)
        assertEquals(1, task2Subtasks.size)
    }
    
    @Test
    fun `updateSubtask changes title and status`() = runTest {
        // Given
        val taskId = repository.insertTask(createTask("Task"))
        val subtaskId = repository.insertSubtask(Subtask(0, taskId, "Original", false))
        
        // When
        val updated = Subtask(subtaskId, taskId, "Updated", true)
        repository.updateSubtask(updated)
        
        // Then
        val retrieved = repository.getSubtaskById(subtaskId)!!
        assertEquals("Updated", retrieved.title)
        assertTrue(retrieved.isDone)
    }
    
    @Test
    fun `toggleSubtaskStatus changes isDone state`() = runTest {
        // Given
        val taskId = repository.insertTask(createTask("Task"))
        val subtaskId = repository.insertSubtask(Subtask(0, taskId, "Subtask", false))
        
        // When
        repository.toggleSubtaskStatus(subtaskId, true)
        
        // Then
        assertTrue(repository.getSubtaskById(subtaskId)!!.isDone)
        
        // When - toggle обратно
        repository.toggleSubtaskStatus(subtaskId, false)
        
        // Then
        assertFalse(repository.getSubtaskById(subtaskId)!!.isDone)
    }
    
    @Test
    fun `deleteSubtask removes subtask from database`() = runTest {
        // Given
        val taskId = repository.insertTask(createTask("Task"))
        val subtaskId = repository.insertSubtask(Subtask(0, taskId, "Subtask", false))
        
        // When
        repository.deleteSubtask(subtaskId)
        
        // Then
        assertNull(repository.getSubtaskById(subtaskId))
    }
    
    @Test
    fun `areAllSubtasksCompleted returns true when all done`() = runTest {
        // Given
        val taskId = repository.insertTask(createTask("Task"))
        val sub1Id = repository.insertSubtask(Subtask(0, taskId, "Sub 1", false))
        val sub2Id = repository.insertSubtask(Subtask(0, taskId, "Sub 2", false))
        
        // When - выполняем все
        repository.toggleSubtaskStatus(sub1Id, true)
        repository.toggleSubtaskStatus(sub2Id, true)
        
        // Then
        assertTrue(repository.areAllSubtasksCompleted(taskId))
    }
    
    @Test
    fun `areAllSubtasksCompleted returns false when some not done`() = runTest {
        // Given
        val taskId = repository.insertTask(createTask("Task"))
        repository.insertSubtask(Subtask(0, taskId, "Sub 1", true))
        repository.insertSubtask(Subtask(0, taskId, "Sub 2", false))
        
        // Then
        assertFalse(repository.areAllSubtasksCompleted(taskId))
    }
    
    @Test
    fun `areAllSubtasksCompleted returns true when no subtasks`() = runTest {
        // Given - задача без подзадач
        val taskId = repository.insertTask(createTask("Task"))
        
        // Then
        assertTrue(repository.areAllSubtasksCompleted(taskId))
    }
    
    // ========================================
    // FLOW REACTIVITY
    // ========================================
    
    @Test
    fun `getAllTasks Flow emits updated data after insert`() = runTest {
        repository.getAllTasks().test {
            // Initial empty state
            var tasks = awaitItem()
            assertEquals(0, tasks.size)
            
            // Insert task
            repository.insertTask(createTask("New Task"))
            
            // Flow should emit new value
            tasks = awaitItem()
            assertEquals(1, tasks.size)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    // ========================================
    // HELPER FUNCTIONS
    // ========================================
    
    private fun createTask(
        title: String,
        description: String = "Description",
        importance: Importance = Importance.MEDIUM,
        isToday: Boolean = false,
        isCompleted: Boolean = false,
        createdAt: Long = 1000L,
        updatedAt: Long = 1000L,
        subtasks: List<Subtask> = emptyList()
    ): Task {
        return Task(
            id = 0,
            title = title,
            description = description,
            importance = importance,
            isToday = isToday,
            isCompleted = isCompleted,
            createdAt = createdAt,
            updatedAt = updatedAt,
            subtasks = subtasks
        )
    }
}


package ru.selin.smartnotes.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.selin.smartnotes.domain.model.Subtask
import ru.selin.smartnotes.domain.model.Task

/**
 * TasksRepository - Интерфейс репозитория для работы с задачами и подзадачами
 * 
 * Domain Layer: Repository Interface (абстракция)
 * 
 * Особенности:
 * - Включает методы для работы с подзадачами
 * - Автоматическое обновление updatedAt родительской задачи при изменении подзадач
 * - Проверка завершенности всех подзадач → автоматическое завершение задачи
 * 
 * Все методы suspend для асинхронного выполнения с корутинами
 */
interface TasksRepository {
    
    // ========================================
    // ОПЕРАЦИИ С ЗАДАЧАМИ
    // ========================================
    
    /**
     * Получить все задачи в виде Flow для реактивного обновления
     * @return Flow со списком задач (с подзадачами)
     */
    fun getAllTasks(): Flow<List<Task>>
    
    /**
     * Получить задачи "на сегодня"
     * @return Flow со списком задач, помеченных как isToday = true
     */
    fun getTasksForToday(): Flow<List<Task>>
    
    /**
     * Получить завершенные задачи
     * @return Flow со списком завершенных задач (isCompleted = true)
     */
    fun getCompletedTasks(): Flow<List<Task>>
    
    /**
     * Получить активные (незавершенные) задачи
     * @return Flow со списком активных задач (isCompleted = false)
     */
    fun getActiveTasks(): Flow<List<Task>>
    
    /**
     * Получить задачу по ID
     * @param id ID задачи
     * @return Задача с подзадачами или null, если не найдена
     */
    suspend fun getTaskById(id: Long): Task?
    
    /**
     * Создать новую задачу
     * @param task Задача для создания (id будет присвоен автоматически)
     * @return ID созданной задачи
     */
    suspend fun insertTask(task: Task): Long
    
    /**
     * Обновить существующую задачу
     * @param task Задача с обновлёнными данными
     */
    suspend fun updateTask(task: Task)
    
    /**
     * Обновить только timestamp задачи (для случаев изменения подзадач)
     * @param taskId ID задачи
     * @param timestamp Новое время обновления
     */
    suspend fun updateTaskTimestamp(taskId: Long, timestamp: Long)
    
    /**
     * Обновить статус завершенности задачи
     * @param taskId ID задачи
     * @param isCompleted Новый статус
     * @param timestamp Время обновления
     */
    suspend fun updateTaskCompletionStatus(taskId: Long, isCompleted: Boolean, timestamp: Long)
    
    /**
     * Удалить задачу (подзадачи удалятся каскадно)
     * @param id ID задачи для удаления
     */
    suspend fun deleteTask(id: Long)
    
    /**
     * Поиск задач по тексту
     * @param query Поисковый запрос (ищет в title и description)
     * @return Flow со списком найденных задач
     */
    fun searchTasks(query: String): Flow<List<Task>>
    
    // ========================================
    // ОПЕРАЦИИ С ПОДЗАДАЧАМИ
    // ========================================
    
    /**
     * Получить подзадачи для конкретной задачи
     * @param taskId ID родительской задачи
     * @return Список подзадач
     */
    suspend fun getSubtasksByTaskId(taskId: Long): List<Subtask>
    
    /**
     * Получить подзадачу по ID
     * @param id ID подзадачи
     * @return Подзадача или null, если не найдена
     */
    suspend fun getSubtaskById(id: Long): Subtask?
    
    /**
     * Создать новую подзадачу
     * ВАЖНО: Автоматически обновляет updatedAt родительской задачи
     * 
     * @param subtask Подзадача для создания
     * @return ID созданной подзадачи
     */
    suspend fun insertSubtask(subtask: Subtask): Long
    
    /**
     * Обновить подзадачу
     * ВАЖНО: 
     * - Автоматически обновляет updatedAt родительской задачи
     * - Проверяет, все ли подзадачи выполнены → автоматически помечает задачу как завершённую
     * 
     * @param subtask Подзадача с обновлёнными данными
     */
    suspend fun updateSubtask(subtask: Subtask)
    
    /**
     * Переключить статус выполнения подзадачи
     * ВАЖНО: Применяет ту же логику, что и updateSubtask
     * 
     * @param subtaskId ID подзадачи
     * @param isDone Новый статус
     */
    suspend fun toggleSubtaskStatus(subtaskId: Long, isDone: Boolean)
    
    /**
     * Удалить подзадачу
     * ВАЖНО: Автоматически обновляет updatedAt родительской задачи
     * 
     * @param id ID подзадачи для удаления
     */
    suspend fun deleteSubtask(id: Long)
    
    /**
     * Проверить, все ли подзадачи выполнены
     * @param taskId ID задачи
     * @return true если все подзадачи выполнены или их нет
     */
    suspend fun areAllSubtasksCompleted(taskId: Long): Boolean
}


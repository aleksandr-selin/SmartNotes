package ru.selin.smartnotes.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.selin.smartnotes.domain.model.Note

/**
 * NotesRepository - Интерфейс репозитория для работы с заметками
 * 
 * Domain Layer: Repository Interface (абстракция)
 * 
 * Принципы SOLID:
 * - Dependency Inversion: domain зависит от абстракции, а не от деталей реализации
 * - Interface Segregation: отдельный интерфейс для заметок, не смешиваем с задачами
 * 
 * Все методы suspend для асинхронного выполнения с корутинами
 * Flow используется для реактивного получения данных (автообновление UI при изменениях)
 */
interface NotesRepository {
    
    /**
     * Получить все заметки в виде Flow для реактивного обновления
     * @param sortByCreatedAt true = сортировка по дате создания, false = по дате обновления
     * @return Flow со списком заметок
     */
    fun getAllNotes(sortByCreatedAt: Boolean = false): Flow<List<Note>>
    
    /**
     * Получить заметку по ID
     * @param id ID заметки
     * @return Заметка или null, если не найдена
     */
    suspend fun getNoteById(id: Long): Note?
    
    /**
     * Создать новую заметку
     * @param note Заметка для создания (id будет присвоен автоматически)
     * @return ID созданной заметки
     */
    suspend fun insertNote(note: Note): Long
    
    /**
     * Обновить существующую заметку
     * @param note Заметка с обновлёнными данными
     */
    suspend fun updateNote(note: Note)
    
    /**
     * Удалить заметку
     * @param id ID заметки для удаления
     */
    suspend fun deleteNote(id: Long)
    
    /**
     * Поиск заметок по тексту
     * @param query Поисковый запрос (ищет в title и content)
     * @return Flow со списком найденных заметок
     */
    fun searchNotes(query: String): Flow<List<Note>>
}


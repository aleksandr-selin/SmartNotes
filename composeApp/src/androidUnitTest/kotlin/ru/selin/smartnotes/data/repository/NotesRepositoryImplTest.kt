package ru.selin.smartnotes.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ru.selin.smartnotes.database.NotesDatabase
import ru.selin.smartnotes.domain.model.Note
import kotlin.test.*

/**
 * Интеграционные тесты для NotesRepositoryImpl
 * 
 * Приоритет 3: CRUD операции и основные функции
 * 
 * Используется in-memory SQLite БД для изоляции тестов
 * Каждый тест работает с чистой БД (setup/teardown)
 * 
 * Покрытие:
 * - Вставка заметок
 * - Получение по ID
 * - Обновление заметок
 * - Удаление заметок
 * - Получение всех заметок
 * - Сортировка (по дате создания/обновления)
 * - Поиск по тексту
 * - Flow реактивность
 */
class NotesRepositoryImplTest {
    
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: NotesDatabase
    private lateinit var repository: NotesRepositoryImpl
    
    @BeforeTest
    fun setup() {
        // Создаём in-memory БД для каждого теста
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        NotesDatabase.Schema.create(driver)
        // Включаем Foreign Keys (хотя для Notes они не критичны, но для консистентности)
        driver.execute(null, "PRAGMA foreign_keys=ON", 0)
        database = NotesDatabase(driver)
        repository = NotesRepositoryImpl(database)
    }
    
    @AfterTest
    fun teardown() {
        driver.close()
    }
    
    // ========================================
    // INSERT TESTS
    // ========================================
    
    @Test
    fun `insertNote returns valid id`() = runTest {
        // Given
        val note = Note(
            id = 0,
            title = "Test Note",
            content = "Test Content",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        
        // When
        val id = repository.insertNote(note)
        
        // Then
        assertTrue(id > 0, "ID должен быть положительным")
    }
    
    @Test
    fun `insertNote with empty content succeeds`() = runTest {
        // Given
        val note = Note(
            id = 0,
            title = "Empty Note",
            content = "",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        
        // When
        val id = repository.insertNote(note)
        
        // Then
        assertTrue(id > 0)
        val retrieved = repository.getNoteById(id)
        assertEquals("", retrieved?.content)
    }
    
    @Test
    fun `insertNote multiple notes returns different ids`() = runTest {
        // Given
        val note1 = Note(0, "Note 1", "Content 1", 1000L, 1000L)
        val note2 = Note(0, "Note 2", "Content 2", 2000L, 2000L)
        
        // When
        val id1 = repository.insertNote(note1)
        val id2 = repository.insertNote(note2)
        
        // Then
        assertNotEquals(id1, id2, "ID должны быть уникальными")
        assertTrue(id2 > id1, "ID должны возрастать")
    }
    
    // ========================================
    // GET BY ID TESTS
    // ========================================
    
    @Test
    fun `getNoteById returns correct note`() = runTest {
        // Given
        val note = Note(0, "My Note", "My Content", 1000L, 2000L)
        val id = repository.insertNote(note)
        
        // When
        val retrieved = repository.getNoteById(id)
        
        // Then
        assertNotNull(retrieved, "Заметка должна быть найдена")
        assertEquals(id, retrieved.id)
        assertEquals("My Note", retrieved.title)
        assertEquals("My Content", retrieved.content)
        assertEquals(1000L, retrieved.createdAt)
        assertEquals(2000L, retrieved.updatedAt)
    }
    
    @Test
    fun `getNoteById with non-existent id returns null`() = runTest {
        // When
        val retrieved = repository.getNoteById(999L)
        
        // Then
        assertNull(retrieved, "Несуществующая заметка должна вернуть null")
    }
    
    @Test
    fun `getNoteById preserves unicode characters`() = runTest {
        // Given
        val note = Note(0, "Заметка 📝", "Содержание на русском 🇷🇺", 1000L, 1000L)
        val id = repository.insertNote(note)
        
        // When
        val retrieved = repository.getNoteById(id)
        
        // Then
        assertNotNull(retrieved)
        assertEquals("Заметка 📝", retrieved.title)
        assertEquals("Содержание на русском 🇷🇺", retrieved.content)
    }
    
    // ========================================
    // UPDATE TESTS
    // ========================================
    
    @Test
    fun `updateNote changes title and content`() = runTest {
        // Given
        val original = Note(0, "Original Title", "Original Content", 1000L, 1000L)
        val id = repository.insertNote(original)
        
        // When
        val updated = Note(id, "Updated Title", "Updated Content", 1000L, 2000L)
        repository.updateNote(updated)
        
        // Then
        val retrieved = repository.getNoteById(id)
        assertNotNull(retrieved)
        assertEquals("Updated Title", retrieved.title)
        assertEquals("Updated Content", retrieved.content)
        assertEquals(2000L, retrieved.updatedAt)
        assertEquals(1000L, retrieved.createdAt, "createdAt не должен измениться")
    }
    
    @Test
    fun `updateNote updates updatedAt timestamp`() = runTest {
        // Given
        val note = Note(0, "Title", "Content", 1000L, 1000L)
        val id = repository.insertNote(note)
        
        // When
        val updated = note.copy(id = id, updatedAt = 5000L)
        repository.updateNote(updated)
        
        // Then
        val retrieved = repository.getNoteById(id)
        assertEquals(5000L, retrieved?.updatedAt)
    }
    
    // ========================================
    // DELETE TESTS
    // ========================================
    
    @Test
    fun `deleteNote removes note from database`() = runTest {
        // Given
        val note = Note(0, "To Delete", "Content", 1000L, 1000L)
        val id = repository.insertNote(note)
        
        // When
        repository.deleteNote(id)
        
        // Then
        val retrieved = repository.getNoteById(id)
        assertNull(retrieved, "Удалённая заметка не должна быть найдена")
    }
    
    @Test
    fun `deleteNote non-existent id does not throw`() = runTest {
        // When & Then (не должно быть исключений)
        repository.deleteNote(999L)
    }
    
    @Test
    fun `deleteNote does not affect other notes`() = runTest {
        // Given
        val note1Id = repository.insertNote(Note(0, "Note 1", "C1", 1000L, 1000L))
        val note2Id = repository.insertNote(Note(0, "Note 2", "C2", 2000L, 2000L))
        val note3Id = repository.insertNote(Note(0, "Note 3", "C3", 3000L, 3000L))
        
        // When
        repository.deleteNote(note2Id)
        
        // Then
        assertNotNull(repository.getNoteById(note1Id))
        assertNull(repository.getNoteById(note2Id))
        assertNotNull(repository.getNoteById(note3Id))
    }
    
    // ========================================
    // GET ALL NOTES TESTS
    // ========================================
    
    @Test
    fun `getAllNotes returns empty list when no notes`() = runTest {
        // When
        val notes = repository.getAllNotes().first()
        
        // Then
        assertTrue(notes.isEmpty(), "Пустая БД должна вернуть пустой список")
    }
    
    @Test
    fun `getAllNotes returns all inserted notes`() = runTest {
        // Given
        repository.insertNote(Note(0, "Note 1", "C1", 1000L, 1000L))
        repository.insertNote(Note(0, "Note 2", "C2", 2000L, 2000L))
        repository.insertNote(Note(0, "Note 3", "C3", 3000L, 3000L))
        
        // When
        val notes = repository.getAllNotes().first()
        
        // Then
        assertEquals(3, notes.size)
    }
    
    @Test
    fun `getAllNotes sorts by updatedAt DESC by default`() = runTest {
        // Given - создаём заметки с разными updatedAt
        repository.insertNote(Note(0, "First", "A", 1000L, 1000L))
        repository.insertNote(Note(0, "Second", "B", 2000L, 3000L)) // самый новый
        repository.insertNote(Note(0, "Third", "C", 3000L, 2000L))
        
        // When
        val notes = repository.getAllNotes(sortByCreatedAt = false).first()
        
        // Then
        assertEquals(3, notes.size)
        assertEquals("Second", notes[0].title, "Самый новый должен быть первым")
        assertEquals("Third", notes[1].title)
        assertEquals("First", notes[2].title)
    }
    
    @Test
    fun `getAllNotes sorts by createdAt DESC when specified`() = runTest {
        // Given
        repository.insertNote(Note(0, "First", "A", 1000L, 5000L))
        repository.insertNote(Note(0, "Second", "B", 2000L, 1000L))
        repository.insertNote(Note(0, "Third", "C", 3000L, 2000L)) // самый новый по createdAt
        
        // When
        val notes = repository.getAllNotes(sortByCreatedAt = true).first()
        
        // Then
        assertEquals("Third", notes[0].title, "Самый новый по createdAt должен быть первым")
        assertEquals("Second", notes[1].title)
        assertEquals("First", notes[2].title)
    }
    
    // ========================================
    // SEARCH TESTS
    // ========================================
    
    @Test
    fun `searchNotes finds notes by title`() = runTest {
        // Given
        repository.insertNote(Note(0, "Kotlin Basics", "Programming", 1000L, 1000L))
        repository.insertNote(Note(0, "Java Tutorial", "Programming", 1000L, 1000L))
        repository.insertNote(Note(0, "Kotlin Advanced", "Programming", 1000L, 1000L))
        
        // When
        val results = repository.searchNotes("Kotlin").first()
        
        // Then
        assertEquals(2, results.size)
        assertTrue(results.all { "Kotlin" in it.title })
    }
    
    @Test
    fun `searchNotes finds notes by content`() = runTest {
        // Given
        repository.insertNote(Note(0, "Note 1", "Learn Android", 1000L, 1000L))
        repository.insertNote(Note(0, "Note 2", "Learn iOS", 1000L, 1000L))
        repository.insertNote(Note(0, "Note 3", "Learn Android and iOS", 1000L, 1000L))
        
        // When
        val results = repository.searchNotes("Android").first()
        
        // Then
        assertEquals(2, results.size)
        assertTrue(results.all { "Android" in it.content })
    }
    
    @Test
    fun `searchNotes is case insensitive`() = runTest {
        // Given
        repository.insertNote(Note(0, "UPPERCASE", "content", 1000L, 1000L))
        repository.insertNote(Note(0, "lowercase", "content", 1000L, 1000L))
        repository.insertNote(Note(0, "MixedCase", "content", 1000L, 1000L))
        
        // When - ищем lowercase
        val results = repository.searchNotes("case").first()
        
        // Then
        assertEquals(3, results.size, "Поиск должен быть case-insensitive")
    }
    
    @Test
    fun `searchNotes with empty query returns all notes`() = runTest {
        // Given
        repository.insertNote(Note(0, "Note 1", "C1", 1000L, 1000L))
        repository.insertNote(Note(0, "Note 2", "C2", 1000L, 1000L))
        
        // When
        val results = repository.searchNotes("").first()
        
        // Then
        assertEquals(2, results.size, "Пустой запрос должен вернуть все заметки")
    }
    
    @Test
    fun `searchNotes with no matches returns empty list`() = runTest {
        // Given
        repository.insertNote(Note(0, "Android", "Kotlin", 1000L, 1000L))
        
        // When
        val results = repository.searchNotes("iOS").first()
        
        // Then
        assertTrue(results.isEmpty())
    }
    
    // ========================================
    // FLOW REACTIVITY TESTS
    // ========================================
    
    @Test
    fun `getAllNotes Flow emits updated data after insert`() = runTest {
        // Given - подписываемся на Flow
        repository.getAllNotes().test {
            // When - первое значение (пустой список)
            var notes = awaitItem()
            assertEquals(0, notes.size)
            
            // When - вставляем заметку
            repository.insertNote(Note(0, "New Note", "Content", 1000L, 1000L))
            
            // Then - Flow должен эмитнуть новое значение
            notes = awaitItem()
            assertEquals(1, notes.size)
            assertEquals("New Note", notes[0].title)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `getAllNotes Flow emits updated data after delete`() = runTest {
        // Given
        val id = repository.insertNote(Note(0, "To Delete", "Content", 1000L, 1000L))
        
        repository.getAllNotes().test {
            // Пропускаем начальное значение
            awaitItem()
            
            // When - удаляем заметку
            repository.deleteNote(id)
            
            // Then - Flow эмитнул обновление
            val notes = awaitItem()
            assertTrue(notes.isEmpty())
            
            cancelAndIgnoreRemainingEvents()
        }
    }
}


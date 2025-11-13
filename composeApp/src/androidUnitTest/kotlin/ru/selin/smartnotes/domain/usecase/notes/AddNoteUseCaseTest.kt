package ru.selin.smartnotes.domain.usecase.notes

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import ru.selin.smartnotes.domain.repository.NotesRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Тесты для AddNoteUseCase
 * 
 * Покрытие:
 * - Валидация пустого title
 * - Успешное создание заметки
 * - Обработка ошибок репозитория
 */
class AddNoteUseCaseTest {
    
    @Test
    fun `adding note with blank title returns failure`() = runTest {
        // Given
        val repository = mockk<NotesRepository>()
        val useCase = AddNoteUseCase(repository)
        
        // When
        val result = useCase(title = "", content = "Content")
        
        // Then
        assertTrue(result.isFailure, "Должна вернуться ошибка")
        assertEquals(
            "Заголовок не может быть пустым",
            result.exceptionOrNull()?.message
        )
        // Репозиторий не должен вызываться
        coVerify(exactly = 0) { repository.insertNote(any()) }
    }
    
    @Test
    fun `adding note with spaces-only title returns failure`() = runTest {
        // Given
        val repository = mockk<NotesRepository>()
        val useCase = AddNoteUseCase(repository)
        
        // When
        val result = useCase(title = "   ", content = "Content")
        
        // Then
        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.insertNote(any()) }
    }
    
    @Test
    fun `adding note with valid data returns success with id`() = runTest {
        // Given
        val repository = mockk<NotesRepository>()
        coEvery { repository.insertNote(any()) } returns 42L
        val useCase = AddNoteUseCase(repository)
        
        // When
        val result = useCase(title = "Test Note", content = "Test Content")
        
        // Then
        assertTrue(result.isSuccess, "Должен вернуться успех")
        assertEquals(42L, result.getOrNull())
        coVerify(exactly = 1) { repository.insertNote(any()) }
    }
    
    @Test
    fun `adding note trims title and content`() = runTest {
        // Given
        val repository = mockk<NotesRepository>()
        coEvery { repository.insertNote(any()) } returns 1L
        val useCase = AddNoteUseCase(repository)
        
        // When
        useCase(title = "  Title  ", content = "  Content  ")
        
        // Then
        coVerify(exactly = 1) {
            repository.insertNote(
                match { note ->
                    note.title == "Title" && note.content == "Content"
                }
            )
        }
    }
    
    @Test
    fun `repository exception is wrapped in Result failure`() = runTest {
        // Given
        val repository = mockk<NotesRepository>()
        val exception = RuntimeException("Database error")
        coEvery { repository.insertNote(any()) } throws exception
        val useCase = AddNoteUseCase(repository)
        
        // When
        val result = useCase(title = "Title", content = "Content")
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}


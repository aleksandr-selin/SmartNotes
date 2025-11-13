package ru.selin.smartnotes.domain.usecase.notes

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import ru.selin.smartnotes.domain.model.Note
import ru.selin.smartnotes.domain.repository.NotesRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Тесты для UpdateNoteUseCase
 * 
 * Покрытие:
 * - Валидация пустого title
 * - Проверка существования заметки
 * - Успешное обновление
 * - Обновление updatedAt
 */
class UpdateNoteUseCaseTest {
    
    @Test
    fun `updating note with blank title returns failure`() = runTest {
        // Given
        val repository = mockk<NotesRepository>()
        val useCase = UpdateNoteUseCase(repository)
        
        // When
        val result = useCase(noteId = 1L, title = "", content = "Content")
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(
            "Заголовок не может быть пустым",
            result.exceptionOrNull()?.message
        )
        coVerify(exactly = 0) { repository.getNoteById(any()) }
        coVerify(exactly = 0) { repository.updateNote(any()) }
    }
    
    @Test
    fun `updating non-existent note returns failure`() = runTest {
        // Given
        val repository = mockk<NotesRepository>()
        coEvery { repository.getNoteById(1L) } returns null
        val useCase = UpdateNoteUseCase(repository)
        
        // When
        val result = useCase(noteId = 1L, title = "Title", content = "Content")
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(
            "Заметка не найдена",
            result.exceptionOrNull()?.message
        )
        coVerify(exactly = 1) { repository.getNoteById(1L) }
        coVerify(exactly = 0) { repository.updateNote(any()) }
    }
    
    @Test
    fun `updating existing note returns success`() = runTest {
        // Given
        val existingNote = Note(
            id = 1L,
            title = "Old Title",
            content = "Old Content",
            createdAt = 1000L,
            updatedAt = 2000L
        )
        val repository = mockk<NotesRepository>()
        coEvery { repository.getNoteById(1L) } returns existingNote
        coEvery { repository.updateNote(any()) } returns Unit
        val useCase = UpdateNoteUseCase(repository)
        
        // When
        val result = useCase(
            noteId = 1L,
            title = "New Title",
            content = "New Content"
        )
        
        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.getNoteById(1L) }
        coVerify(exactly = 1) { repository.updateNote(any()) }
    }
    
    @Test
    fun `updating note trims title and content`() = runTest {
        // Given
        val existingNote = Note(
            id = 1L,
            title = "Title",
            content = "Content",
            createdAt = 1000L,
            updatedAt = 2000L
        )
        val repository = mockk<NotesRepository>()
        coEvery { repository.getNoteById(1L) } returns existingNote
        coEvery { repository.updateNote(any()) } returns Unit
        val useCase = UpdateNoteUseCase(repository)
        
        // When
        useCase(noteId = 1L, title = "  New Title  ", content = "  New Content  ")
        
        // Then
        coVerify(exactly = 1) {
            repository.updateNote(
                match { note ->
                    note.title == "New Title" && note.content == "New Content"
                }
            )
        }
    }
    
    @Test
    fun `updating note updates updatedAt timestamp`() = runTest {
        // Given
        val existingNote = Note(
            id = 1L,
            title = "Title",
            content = "Content",
            createdAt = 1000L,
            updatedAt = 2000L
        )
        val repository = mockk<NotesRepository>()
        coEvery { repository.getNoteById(1L) } returns existingNote
        coEvery { repository.updateNote(any()) } returns Unit
        val useCase = UpdateNoteUseCase(repository)
        
        // When
        useCase(noteId = 1L, title = "New Title", content = "New Content")
        
        // Then
        coVerify(exactly = 1) {
            repository.updateNote(
                match { note ->
                    note.updatedAt > 2000L // updatedAt должен быть обновлён
                }
            )
        }
    }
    
    @Test
    fun `updating note preserves createdAt`() = runTest {
        // Given
        val existingNote = Note(
            id = 1L,
            title = "Title",
            content = "Content",
            createdAt = 1000L,
            updatedAt = 2000L
        )
        val repository = mockk<NotesRepository>()
        coEvery { repository.getNoteById(1L) } returns existingNote
        coEvery { repository.updateNote(any()) } returns Unit
        val useCase = UpdateNoteUseCase(repository)
        
        // When
        useCase(noteId = 1L, title = "New Title", content = "New Content")
        
        // Then
        coVerify(exactly = 1) {
            repository.updateNote(
                match { note ->
                    note.createdAt == 1000L // createdAt не должен измениться
                }
            )
        }
    }
}


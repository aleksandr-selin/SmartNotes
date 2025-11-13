package ru.selin.smartnotes.domain.usecase.notes

import ru.selin.smartnotes.domain.repository.NotesRepository

/**
 * Use Case для удаления заметки
 * 
 * Domain Layer: Business Logic
 * 
 * @param notesRepository Репозиторий заметок (внедряется через DI)
 */
class DeleteNoteUseCase(
    private val notesRepository: NotesRepository
) {
    /**
     * Удаляет заметку по ID
     * 
     * @param noteId ID заметки для удаления
     * @return Result с успехом или ошибкой
     */
    suspend operator fun invoke(noteId: Long): Result<Unit> {
        return try {
            notesRepository.deleteNote(noteId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


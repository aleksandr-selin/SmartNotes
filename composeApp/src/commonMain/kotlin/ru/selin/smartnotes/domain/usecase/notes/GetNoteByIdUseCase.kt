package ru.selin.smartnotes.domain.usecase.notes

import ru.selin.smartnotes.domain.model.Note
import ru.selin.smartnotes.domain.repository.NotesRepository

/**
 * Use Case для получения заметки по ID
 * 
 * Domain Layer: Business Logic
 * 
 * @param notesRepository Репозиторий заметок (внедряется через DI)
 */
class GetNoteByIdUseCase(
    private val notesRepository: NotesRepository
) {
    /**
     * Получает заметку по ID
     * 
     * @param noteId ID заметки
     * @return Заметка или null если не найдена
     */
    suspend operator fun invoke(noteId: Long): Note? {
        return notesRepository.getNoteById(noteId)
    }
}


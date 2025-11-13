package ru.selin.smartnotes.domain.usecase.notes

import kotlinx.coroutines.flow.Flow
import ru.selin.smartnotes.domain.model.Note
import ru.selin.smartnotes.domain.repository.NotesRepository

/**
 * Use Case для получения всех заметок
 * 
 * Domain Layer: Business Logic
 * 
 * Возвращает Flow для реактивного обновления UI
 * 
 * @param notesRepository Репозиторий заметок (внедряется через DI)
 */
class GetAllNotesUseCase(
    private val notesRepository: NotesRepository
) {
    /**
     * Получает все заметки с сортировкой
     * 
     * @param sortByCreatedAt true = по дате создания, false = по дате обновления (по умолчанию)
     * @return Flow со списком заметок
     */
    operator fun invoke(sortByCreatedAt: Boolean = false): Flow<List<Note>> {
        return notesRepository.getAllNotes(sortByCreatedAt)
    }
}


package ru.selin.smartnotes.domain.usecase.notes

import kotlinx.coroutines.flow.Flow
import ru.selin.smartnotes.domain.model.Note
import ru.selin.smartnotes.domain.repository.NotesRepository

/**
 * Use Case для поиска заметок по тексту
 * 
 * Domain Layer: Business Logic
 * 
 * Поиск выполняется по title и content (case-insensitive)
 * 
 * @param notesRepository Репозиторий заметок (внедряется через DI)
 */
class SearchNotesUseCase(
    private val notesRepository: NotesRepository
) {
    /**
     * Ищет заметки по поисковому запросу
     * 
     * @param query Поисковый запрос
     * @return Flow со списком найденных заметок
     */
    operator fun invoke(query: String): Flow<List<Note>> {
        return notesRepository.searchNotes(query.trim())
    }
}


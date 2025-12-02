package ru.selin.smartnotes.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.selin.smartnotes.data.mapper.toDomain
import ru.selin.smartnotes.data.mapper.toInsertParams
import ru.selin.smartnotes.database.NotesDatabase
import ru.selin.smartnotes.domain.model.Note
import ru.selin.smartnotes.domain.repository.NotesRepository

/**
 * NotesRepositoryImpl - Реализация репозитория для работы с заметками
 *
 * Data Layer: Repository Implementation
 *
 * Принципы:
 * - Все операции с БД выполняются на Dispatchers.IO
 * - Использует Flow для реактивного получения данных
 * - Применяет мапперы для преобразования между БД entities и domain моделями
 * - Инкапсулирует логику работы с SQLDelight, скрывая детали от domain слоя
 *
 * @param database Экземпляр NotesDatabase (внедряется через Koin)
 */
class NotesRepositoryImpl(
    private val database: NotesDatabase
) : NotesRepository {

    private val queries = database.noteQueries

    override fun getAllNotes(sortByCreatedAt: Boolean): Flow<List<Note>> {
        return if (sortByCreatedAt) {
            queries.getAllNotesByCreatedAt()
                .asFlow()
                .mapToList(Dispatchers.IO)
                .map { notes -> notes.map { it.toDomain() } }
        } else {
            queries.getAllNotes()
                .asFlow()
                .mapToList(Dispatchers.IO)
                .map { notes -> notes.map { it.toDomain() } }
        }
    }

    override suspend fun getNoteById(id: Long): Note? = withContext(Dispatchers.IO) {
        queries.getNoteById(id)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override suspend fun insertNote(note: Note): Long = withContext(Dispatchers.IO) {
        val params = note.toInsertParams()
        
        // Вставляем заметку
        queries.insertNote(
            title = params.title,
            content = params.content,
            createdAt = params.createdAt,
            updatedAt = params.updatedAt
        )
        
        // Получаем ID последней вставленной записи
        queries.getLastInsertedNoteId().executeAsOne()
    }

    override suspend fun updateNote(note: Note): Unit = withContext(Dispatchers.IO) {
        queries.updateNote(
            title = note.title,
            content = note.content,
            updatedAt = note.updatedAt,
            id = note.id
        )
    }

    override suspend fun deleteNote(id: Long): Unit = withContext(Dispatchers.IO) {
        queries.deleteNote(id)
    }

    override fun searchNotes(query: String): Flow<List<Note>> {
        return queries.searchNotes(query, query) // Передаём дважды для title и content
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { notes -> notes.map { it.toDomain() } }
    }
}


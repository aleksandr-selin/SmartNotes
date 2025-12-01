package ru.selin.smartnotes.presentation.screens.notes

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.selin.smartnotes.domain.model.Note
import ru.selin.smartnotes.domain.usecase.notes.DeleteNoteUseCase
import ru.selin.smartnotes.domain.usecase.notes.GetAllNotesUseCase
import ru.selin.smartnotes.domain.usecase.notes.SearchNotesUseCase

/**
 * ViewModel для экрана списка заметок
 *
 * Использует Voyager ScreenModel для управления состоянием
 */
class NotesListViewModel(
    private val getAllNotesUseCase: GetAllNotesUseCase,
    private val searchNotesUseCase: SearchNotesUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase
) : ScreenModel {

    private val _uiState = MutableStateFlow<NotesListUiState>(NotesListUiState.Loading)
    val uiState: StateFlow<NotesListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadNotes()
    }

    /**
     * Загружает список заметок
     */
    fun loadNotes(sortByCreatedAt: Boolean = false) {
        screenModelScope.launch {
            _uiState.value = NotesListUiState.Loading

            getAllNotesUseCase(sortByCreatedAt = sortByCreatedAt).collect { notes ->
                _uiState.value = if (notes.isEmpty()) {
                    NotesListUiState.Empty
                } else {
                    NotesListUiState.Success(notes)
                }
            }
        }
    }

    /**
     * Поиск заметок по запросу
     */
    fun searchNotes(query: String) {
        _searchQuery.value = query

        if (query.isBlank()) {
            loadNotes()
            return
        }

        screenModelScope.launch {
            _uiState.value = NotesListUiState.Loading

            searchNotesUseCase(query).collect { notes ->
                _uiState.value = if (notes.isEmpty()) {
                    NotesListUiState.Empty
                } else {
                    NotesListUiState.Success(notes)
                }
            }
        }
    }

    /**
     * Удаляет заметку
     */
    fun deleteNote(noteId: Long) {
        screenModelScope.launch {
            deleteNoteUseCase(noteId).fold(
                onSuccess = {
                    // Список обновится автоматически через Flow
                },
                onFailure = { error ->
                    _uiState.value = NotesListUiState.Error(
                        error.message ?: "Ошибка удаления"
                    )
                }
            )
        }
    }

    /**
     * Очищает поисковый запрос
     */
    fun clearSearch() {
        _searchQuery.value = ""
        loadNotes()
    }
}

/**
 * UI состояния для экрана списка заметок
 */
sealed class NotesListUiState {
    data object Loading : NotesListUiState()
    data object Empty : NotesListUiState()
    data class Success(val notes: List<Note>) : NotesListUiState()
    data class Error(val message: String) : NotesListUiState()
}


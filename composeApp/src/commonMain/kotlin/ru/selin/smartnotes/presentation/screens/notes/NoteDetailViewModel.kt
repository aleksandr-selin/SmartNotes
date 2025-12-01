package ru.selin.smartnotes.presentation.screens.notes

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.selin.smartnotes.domain.usecase.notes.AddNoteUseCase
import ru.selin.smartnotes.domain.usecase.notes.DeleteNoteUseCase
import ru.selin.smartnotes.domain.usecase.notes.GetNoteByIdUseCase
import ru.selin.smartnotes.domain.usecase.notes.UpdateNoteUseCase

/**
 * ViewModel для экрана детальной информации заметки
 *
 * Поддерживает создание и редактирование заметки
 */
class NoteDetailViewModel(
    private val noteId: Long?,
    private val getNoteByIdUseCase: GetNoteByIdUseCase,
    private val addNoteUseCase: AddNoteUseCase,
    private val updateNoteUseCase: UpdateNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase
) : ScreenModel {

    private val _uiState = MutableStateFlow<NoteDetailUiState>(NoteDetailUiState.Loading)
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _canSave = MutableStateFlow(false)
    val canSave: StateFlow<Boolean> = _canSave.asStateFlow()

    val isEditMode: Boolean = noteId != null

    init {
        if (noteId != null) {
            loadNote(noteId)
        } else {
            _uiState.value = NoteDetailUiState.Success
        }
    }

    /**
     * Загружает заметку по ID
     */
    private fun loadNote(id: Long) {
        screenModelScope.launch {
            _uiState.value = NoteDetailUiState.Loading
            try {
                val note = getNoteByIdUseCase(id)
                if (note != null) {
                    _title.value = note.title
                    _content.value = note.content
                    updateCanSave()
                    _uiState.value = NoteDetailUiState.Success
                } else {
                    _uiState.value = NoteDetailUiState.Error("Заметка не найдена")
                }
            } catch (e: Exception) {
                _uiState.value = NoteDetailUiState.Error(
                    e.message ?: "Ошибка загрузки"
                )
            }
        }
    }

    /**
     * Обновляет заголовок заметки
     */
    fun updateTitle(newTitle: String) {
        _title.value = newTitle
        updateCanSave()
    }

    /**
     * Обновляет содержимое заметки
     */
    fun updateContent(newContent: String) {
        _content.value = newContent
        updateCanSave()
    }

    /**
     * Обновляет состояние возможности сохранения
     */
    private fun updateCanSave() {
        _canSave.value = _title.value.isNotBlank() && _content.value.isNotBlank()
    }

    /**
     * Сохраняет заметку (создание или обновление)
     */
    fun saveNote(onSuccess: () -> Unit) {
        if (_isSaving.value) return

        screenModelScope.launch {
            _isSaving.value = true

            val result = if (isEditMode && noteId != null) {
                // Режим редактирования
                updateNoteUseCase(
                    noteId = noteId,
                    title = _title.value,
                    content = _content.value
                )
            } else {
                // Режим создания
                addNoteUseCase(
                    title = _title.value,
                    content = _content.value
                )
            }

            result.fold(
                onSuccess = {
                    _isSaving.value = false
                    onSuccess()
                },
                onFailure = { error ->
                    _isSaving.value = false
                    _uiState.value = NoteDetailUiState.Error(
                        error.message ?: "Ошибка сохранения"
                    )
                }
            )
        }
    }

    /**
     * Проверяет, можно ли сохранить заметку
     * @deprecated Используйте canSave StateFlow вместо этого метода
     */
    @Deprecated("Use canSave StateFlow instead", ReplaceWith("canSave.value"))
    fun canSave(): Boolean {
        return _title.value.isNotBlank() && _content.value.isNotBlank()
    }

    /**
     * Удаляет текущую заметку
     */
    fun deleteNote(onSuccess: () -> Unit) {
        if (noteId == null || !isEditMode) return

        screenModelScope.launch {
            deleteNoteUseCase(noteId).fold(
                onSuccess = {
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.value = NoteDetailUiState.Error(
                        error.message ?: "Ошибка удаления"
                    )
                }
            )
        }
    }
}

/**
 * UI состояния для экрана детальной информации заметки
 */
sealed class NoteDetailUiState {
    data object Loading : NoteDetailUiState()
    data object Success : NoteDetailUiState()
    data class Error(val message: String) : NoteDetailUiState()
}


package ru.selin.smartnotes.domain.usecase.notes

import ru.selin.smartnotes.domain.repository.NotesRepository
import kotlin.time.Clock

/**
 * Use Case для обновления заметки
 *
 * Domain Layer: Business Logic
 *
 * Бизнес-правила:
 * - Автоматически обновляет updatedAt
 * - Валидирует title
 * - Проверяет существование заметки
 *
 * @param notesRepository Репозиторий заметок (внедряется через DI)
 */
class UpdateNoteUseCase(
    private val notesRepository: NotesRepository
) {
    /**
     * Обновляет существующую заметку
     *
     * @param noteId ID заметки
     * @param title Новый заголовок
     * @param content Новое содержимое
     * @return Result с успехом или ошибкой
     */
    suspend operator fun invoke(
        noteId: Long,
        title: String,
        content: String
    ): Result<Unit> {
        return try {
            // Валидация
            if (title.isBlank()) {
                return Result.failure(IllegalArgumentException("Заголовок не может быть пустым"))
            }

            // Получаем существующую заметку
            val existingNote = notesRepository.getNoteById(noteId)
                ?: return Result.failure(IllegalArgumentException("Заметка не найдена"))

            // Обновляем с новым updatedAt
            val updatedNote = existingNote.copy(
                title = title.trim(),
                content = content.trim(),
                updatedAt = Clock.System.now().epochSeconds
            )

            notesRepository.updateNote(updatedNote)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


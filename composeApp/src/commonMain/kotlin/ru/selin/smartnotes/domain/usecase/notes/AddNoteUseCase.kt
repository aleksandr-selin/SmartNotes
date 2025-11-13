package ru.selin.smartnotes.domain.usecase.notes

import ru.selin.smartnotes.domain.model.Note
import ru.selin.smartnotes.domain.repository.NotesRepository
import kotlin.time.Clock

/**
 * Use Case для создания новой заметки
 *
 * Domain Layer: Business Logic
 *
 * Бизнес-правила:
 * - Автоматически устанавливает createdAt и updatedAt
 * - Валидирует, что title не пустой
 * - Возвращает Result для обработки ошибок
 *
 * @param notesRepository Репозиторий заметок (внедряется через DI)
 */
class AddNoteUseCase(
    private val notesRepository: NotesRepository
) {
    /**
     * Создаёт новую заметку
     *
     * @param title Заголовок заметки
     * @param content Содержимое заметки
     * @return Result с ID созданной заметки или ошибкой
     */
    suspend operator fun invoke(
        title: String,
        content: String
    ): Result<Long> {
        return try {
            // Валидация
            if (title.isBlank()) {
                return Result.failure(IllegalArgumentException("Заголовок не может быть пустым"))
            }

            val currentTime = Clock.System.now().epochSeconds
            val note = Note(
                id = 0, // Будет присвоен БД
                title = title.trim(),
                content = content.trim(),
                createdAt = currentTime,
                updatedAt = currentTime
            )

            val noteId = notesRepository.insertNote(note)
            Result.success(noteId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


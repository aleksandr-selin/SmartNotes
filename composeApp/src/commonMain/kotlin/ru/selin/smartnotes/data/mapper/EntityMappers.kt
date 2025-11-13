package ru.selin.smartnotes.data.mapper

import ru.selin.smartnotes.domain.model.Importance
import ru.selin.smartnotes.domain.model.Note
import ru.selin.smartnotes.domain.model.Subtask
import ru.selin.smartnotes.domain.model.Task
import ru.selin.smartnotes.database.Note as NoteEntity
import ru.selin.smartnotes.database.Subtask as SubtaskEntity
import ru.selin.smartnotes.database.Task as TaskEntity

/**
 * EntityMappers - Мапперы для преобразования между БД entities и domain моделями
 *
 * Data Layer: Mapping
 *
 * Почему нужны мапперы:
 * - Разделение concerns: domain модели не зависят от деталей БД
 * - SQLite хранит Boolean как INTEGER (0/1), а domain использует Boolean
 * - SQLite хранит Enum как TEXT, а domain использует Kotlin Enum
 * - Domain модели могут содержать бизнес-логику и вычисляемые поля
 */

// ========================================
// NOTE MAPPERS
// ========================================

/**
 * Преобразует Note entity из БД в domain модель
 */
fun NoteEntity.toDomain(): Note {
    return Note(
        id = this.id,
        title = this.title,
        content = this.content,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

/**
 * Преобразует domain модель Note в параметры для вставки в БД
 * (SQLDelight сам создаст entity при выполнении запроса)
 */
fun Note.toInsertParams(): NoteInsertParams {
    return NoteInsertParams(
        title = this.title,
        content = this.content,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

// Вспомогательный класс для параметров вставки
data class NoteInsertParams(
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)

// ========================================
// TASK MAPPERS
// ========================================

/**
 * Преобразует Task entity из БД в domain модель
 * @param subtasks Подзадачи для этой задачи (передаются отдельно, т.к. это связанная таблица)
 */
fun TaskEntity.toDomain(subtasks: List<Subtask> = emptyList()): Task {
    return Task(
        id = this.id,
        title = this.title,
        description = this.description,
        importance = Importance.fromString(this.importance),
        isToday = this.isToday.toBoolean(),
        isCompleted = this.isCompleted.toBoolean(),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        subtasks = subtasks
    )
}

/**
 * Преобразует domain модель Task в параметры для вставки в БД
 */
fun Task.toInsertParams(): TaskInsertParams {
    return TaskInsertParams(
        title = this.title,
        description = this.description,
        importance = this.importance.name,
        isToday = this.isToday.toLong(),
        isCompleted = this.isCompleted.toLong(),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

// Вспомогательный класс для параметров вставки
data class TaskInsertParams(
    val title: String,
    val description: String,
    val importance: String,
    val isToday: Long,
    val isCompleted: Long,
    val createdAt: Long,
    val updatedAt: Long
)

// ========================================
// SUBTASK MAPPERS
// ========================================

/**
 * Преобразует Subtask entity из БД в domain модель
 */
fun SubtaskEntity.toDomain(): Subtask {
    return Subtask(
        id = this.id,
        taskId = this.taskId,
        title = this.title,
        isDone = this.isDone.toBoolean()
    )
}

/**
 * Преобразует domain модель Subtask в параметры для вставки в БД
 */
fun Subtask.toInsertParams(): SubtaskInsertParams {
    return SubtaskInsertParams(
        taskId = this.taskId,
        title = this.title,
        isDone = this.isDone.toLong()
    )
}

// Вспомогательный класс для параметров вставки
data class SubtaskInsertParams(
    val taskId: Long,
    val title: String,
    val isDone: Long
)

// ========================================
// HELPER FUNCTIONS: TYPE CONVERSIONS
// ========================================

/**
 * Преобразует Long (0 или 1) в Boolean
 * SQLite не имеет Boolean типа, используется INTEGER
 */
fun Long.toBoolean(): Boolean = this != 0L

/**
 * Преобразует Boolean в Long (0 или 1) для SQLite
 */
fun Boolean.toLong(): Long = if (this) 1L else 0L


package ru.selin.smartnotes.domain.model

import kotlinx.serialization.Serializable

/**
 * Note - Доменная модель заметки
 * 
 * Обычная текстовая запись с заголовком и содержимым
 * 
 * Domain Layer: Business Model
 * 
 * @property id Уникальный идентификатор заметки
 * @property title Заголовок заметки
 * @property content Текстовое содержимое заметки
 * @property createdAt Время создания (timestamp в миллисекундах)
 * @property updatedAt Время последнего обновления (timestamp в миллисекундах)
 */
@Serializable
data class Note(
    val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)


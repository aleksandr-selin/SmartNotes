package ru.selin.smartnotes.domain.model

import kotlinx.serialization.Serializable

/**
 * Subtask - Доменная модель подзадачи
 * 
 * Шаг (подзадача) внутри задачи, который можно отметить как выполненный
 * 
 * Domain Layer: Business Model
 * 
 * @property id Уникальный идентификатор подзадачи
 * @property taskId ID родительской задачи
 * @property title Название подзадачи
 * @property isDone Статус выполнения (true = выполнено, false = не выполнено)
 */
@Serializable
data class Subtask(
    val id: Long = 0,
    val taskId: Long,
    val title: String,
    val isDone: Boolean = false
)


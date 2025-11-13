package ru.selin.smartnotes.domain.model

import kotlinx.serialization.Serializable

/**
 * Task - Доменная модель задачи
 * 
 * Задача с логикой выполнения, важностью и подзадачами
 * 
 * Domain Layer: Business Model
 * 
 * Бизнес-правила:
 * - updatedAt обновляется при любом изменении задачи или её подзадач
 * - Когда все подзадачи выполнены, задача автоматически помечается как isCompleted = true
 * - При завершении задачи показывается диалог: "Удалить или оставить запись"
 * 
 * @property id Уникальный идентификатор задачи
 * @property title Заголовок задачи
 * @property description Подробное описание задачи
 * @property importance Уровень важности (LOW, MEDIUM, HIGH)
 * @property isToday Пометка "на сегодня" для быстрого доступа
 * @property isCompleted Статус завершенности
 * @property createdAt Время создания (timestamp в миллисекундах)
 * @property updatedAt Время последнего обновления (timestamp в миллисекундах)
 * @property subtasks Список подзадач (по умолчанию пустой)
 */
@Serializable
data class Task(
    val id: Long = 0,
    val title: String,
    val description: String,
    val importance: Importance = Importance.MEDIUM,
    val isToday: Boolean = false,
    val isCompleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val subtasks: List<Subtask> = emptyList()
) {
    /**
     * Проверяет, все ли подзадачи выполнены
     * @return true если нет подзадач или все выполнены, false иначе
     */
    fun areAllSubtasksCompleted(): Boolean {
        return subtasks.isEmpty() || subtasks.all { it.isDone }
    }
    
    /**
     * Возвращает количество выполненных подзадач
     */
    fun getCompletedSubtasksCount(): Int {
        return subtasks.count { it.isDone }
    }
    
    /**
     * Возвращает прогресс выполнения подзадач (0.0 до 1.0)
     */
    fun getSubtasksProgress(): Float {
        if (subtasks.isEmpty()) return 0f
        return getCompletedSubtasksCount().toFloat() / subtasks.size.toFloat()
    }
}


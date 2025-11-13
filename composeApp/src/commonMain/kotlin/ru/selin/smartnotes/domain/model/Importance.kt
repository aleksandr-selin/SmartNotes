package ru.selin.smartnotes.domain.model

import kotlinx.serialization.Serializable

/**
 * Importance - Уровни важности задачи
 * 
 * Domain Layer: Business Model
 */
@Serializable
enum class Importance {
    LOW,     // Низкая важность
    MEDIUM,  // Средняя важность
    HIGH;    // Высокая важность
    
    companion object {
        /**
         * Преобразует строку в Importance
         * @param value Строковое представление (LOW, MEDIUM, HIGH)
         * @return Importance или LOW по умолчанию
         */
        fun fromString(value: String): Importance {
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                LOW
            }
        }
    }
}


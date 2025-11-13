package ru.selin.smartnotes.data.local

import app.cash.sqldelight.db.SqlDriver

/**
 * DatabaseDriverFactory - паттерн expect/actual для создания платформозависимых драйверов SQLDelight
 * 
 * Это мультиплатформенный подход, где:
 * - Android использует AndroidSqliteDriver
 * - iOS использует NativeSqliteDriver
 * 
 * Data Layer: Local Storage
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}


package ru.selin.smartnotes.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import ru.selin.smartnotes.database.NotesDatabase

/**
 * iOS-реализация DatabaseDriverFactory
 * 
 * Использует NativeSqliteDriver для работы с SQLite на iOS
 */
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = NotesDatabase.Schema,
            name = "notes.db"
        )
    }
}


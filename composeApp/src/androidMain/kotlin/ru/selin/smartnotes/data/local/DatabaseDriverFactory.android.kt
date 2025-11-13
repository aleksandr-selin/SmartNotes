package ru.selin.smartnotes.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import ru.selin.smartnotes.database.NotesDatabase

/**
 * Android-реализация DatabaseDriverFactory
 * 
 * Использует AndroidSqliteDriver для работы с SQLite на Android
 * 
 * @param context Application context для создания БД
 */
actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = NotesDatabase.Schema,
            context = context,
            name = "notes.db"
        )
    }
}


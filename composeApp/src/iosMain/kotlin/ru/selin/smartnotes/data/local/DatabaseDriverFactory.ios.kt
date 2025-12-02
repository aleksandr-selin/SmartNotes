package ru.selin.smartnotes.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.wrapConnection
import co.touchlab.sqliter.DatabaseConfiguration
import ru.selin.smartnotes.database.NotesDatabase

/**
 * iOS-реализация DatabaseDriverFactory
 * 
 * Использует NativeSqliteDriver с правильной конфигурацией для многопоточности.
 * 
 * На iOS SQLite требует специальной настройки для работы с корутинами:
 * - inMemory = false - использовать файловую БД
 * - Драйвер создает connection pool для безопасной работы из разных потоков
 */
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            configuration = DatabaseConfiguration(
                name = "notes.db",
                version = NotesDatabase.Schema.version.toInt(),
                create = { connection ->
                    wrapConnection(connection) { NotesDatabase.Schema.create(it) }
                },
                upgrade = { connection, oldVersion, newVersion ->
                    wrapConnection(connection) { 
                        NotesDatabase.Schema.migrate(it, oldVersion.toLong(), newVersion.toLong()) 
                    }
                },
                inMemory = false
            )
        )
    }
}


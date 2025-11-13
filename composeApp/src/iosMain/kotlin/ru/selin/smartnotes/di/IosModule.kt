package ru.selin.smartnotes.di

import org.koin.dsl.module
import ru.selin.smartnotes.data.local.DatabaseDriverFactory

/**
 * iOS-специфичный модуль
 * 
 * Содержит:
 * - DatabaseDriverFactory для iOS (не требует зависимостей)
 */
val iosModule = module {
    // DatabaseDriverFactory - для создания SQLDelight драйвера на iOS
    single {
        DatabaseDriverFactory()
    }
}


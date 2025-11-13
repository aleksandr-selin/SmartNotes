package ru.selin.smartnotes.di

import org.koin.dsl.module
import ru.selin.smartnotes.data.local.DatabaseDriverFactory

/**
 * Android-специфичный модуль
 * 
 * Содержит:
 * - DatabaseDriverFactory для Android (требует Context)
 */
val androidModule = module {
    // DatabaseDriverFactory - для создания SQLDelight драйвера на Android
    single {
        DatabaseDriverFactory(context = get())
    }
}


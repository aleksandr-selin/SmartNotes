package ru.selin.smartnotes.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.selin.smartnotes.data.local.DatabaseDriverFactory
import ru.selin.smartnotes.data.local.createDatabase
import ru.selin.smartnotes.data.repository.NotesRepositoryImpl
import ru.selin.smartnotes.data.repository.TasksRepositoryImpl
import ru.selin.smartnotes.database.NotesDatabase
import ru.selin.smartnotes.domain.repository.NotesRepository
import ru.selin.smartnotes.domain.repository.TasksRepository

/**
 * Модуль для Data слоя
 * 
 * Содержит:
 * - DatabaseDriverFactory (platform-specific)
 * - NotesDatabase (SQLDelight)
 * - Репозитории (NotesRepository, TasksRepository)
 */
val dataModule = module {
    // DatabaseDriverFactory - будет предоставлен platform-specific модулем
    // (см. androidModule.kt и iosModule.kt)
    
    // NotesDatabase - синглтон SQLDelight базы данных
    single<NotesDatabase> {
        createDatabase(get())
    }
    
    // NotesRepository - реализация репозитория для заметок
    singleOf(::NotesRepositoryImpl) bind NotesRepository::class
    
    // TasksRepository - реализация репозитория для задач и подзадач
    singleOf(::TasksRepositoryImpl) bind TasksRepository::class
}


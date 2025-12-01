package ru.selin.smartnotes.di

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import ru.selin.smartnotes.presentation.screens.notes.NoteDetailViewModel
import ru.selin.smartnotes.presentation.screens.notes.NotesListViewModel
import ru.selin.smartnotes.presentation.screens.tasks.TaskDetailViewModel
import ru.selin.smartnotes.presentation.screens.tasks.TasksListViewModel

/**
 * Модуль для Presentation слоя
 * 
 * Содержит все ScreenModels (ViewModels) для экранов приложения.
 * 
 * Используем factory scope, так как ViewModels создаются для каждого экрана
 * и управляются Voyager ScreenModel lifecycle.
 */
val presentationModule = module {
    
    // ========================================
    // SCREEN MODELS ДЛЯ ЗАМЕТОК (NOTES)
    // ========================================
    
    /**
     * ViewModel для списка заметок
     */
    factoryOf(::NotesListViewModel)
    
    /**
     * ViewModel для детальной информации заметки
     * Параметр noteId передаётся через Voyager Screen
     */
    factory { params ->
        NoteDetailViewModel(
            noteId = params.getOrNull(),
            getNoteByIdUseCase = get(),
            addNoteUseCase = get(),
            updateNoteUseCase = get(),
            deleteNoteUseCase = get()
        )
    }
    
    // ========================================
    // SCREEN MODELS ДЛЯ ЗАДАЧ (TASKS)
    // ========================================
    
    /**
     * ViewModel для списка задач
     */
    factoryOf(::TasksListViewModel)
    
    /**
     * ViewModel для детальной информации задачи
     * Параметр taskId передаётся через Voyager Screen
     */
    factory { params ->
        TaskDetailViewModel(
            taskId = params.getOrNull(),
            getTaskByIdUseCase = get(),
            addTaskUseCase = get(),
            updateTaskUseCase = get(),
            addSubtaskUseCase = get(),
            toggleSubtaskUseCase = get(),
            deleteSubtaskUseCase = get(),
            completeTaskUseCase = get(),
            deleteTaskUseCase = get()
        )
    }
}


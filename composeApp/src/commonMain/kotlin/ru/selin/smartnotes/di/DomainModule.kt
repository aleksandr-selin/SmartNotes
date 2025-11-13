package ru.selin.smartnotes.di

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import ru.selin.smartnotes.domain.usecase.notes.AddNoteUseCase
import ru.selin.smartnotes.domain.usecase.notes.DeleteNoteUseCase
import ru.selin.smartnotes.domain.usecase.notes.GetAllNotesUseCase
import ru.selin.smartnotes.domain.usecase.notes.GetNoteByIdUseCase
import ru.selin.smartnotes.domain.usecase.notes.SearchNotesUseCase
import ru.selin.smartnotes.domain.usecase.notes.UpdateNoteUseCase
import ru.selin.smartnotes.domain.usecase.subtasks.AddSubtaskUseCase
import ru.selin.smartnotes.domain.usecase.subtasks.DeleteSubtaskUseCase
import ru.selin.smartnotes.domain.usecase.subtasks.ToggleSubtaskUseCase
import ru.selin.smartnotes.domain.usecase.subtasks.UpdateSubtaskUseCase
import ru.selin.smartnotes.domain.usecase.tasks.AddTaskUseCase
import ru.selin.smartnotes.domain.usecase.tasks.CompleteTaskUseCase
import ru.selin.smartnotes.domain.usecase.tasks.DeleteTaskUseCase
import ru.selin.smartnotes.domain.usecase.tasks.GetAllTasksUseCase
import ru.selin.smartnotes.domain.usecase.tasks.GetTaskByIdUseCase
import ru.selin.smartnotes.domain.usecase.tasks.GetTasksForTodayUseCase
import ru.selin.smartnotes.domain.usecase.tasks.UpdateTaskUseCase

/**
 * Модуль для Domain слоя
 * 
 * Содержит все Use Cases для работы с:
 * - Заметками (Notes)
 * - Задачами (Tasks)
 * - Подзадачами (Subtasks)
 * 
 * Используем factory scope, так как Use Cases могут вызываться многократно
 * и не требуют сохранения состояния между вызовами.
 */
val domainModule = module {
    // ========================================
    // USE CASES ДЛЯ ЗАМЕТОК (NOTES)
    // ========================================
    factoryOf(::AddNoteUseCase)
    factoryOf(::GetAllNotesUseCase)
    factoryOf(::GetNoteByIdUseCase)
    factoryOf(::UpdateNoteUseCase)
    factoryOf(::DeleteNoteUseCase)
    factoryOf(::SearchNotesUseCase)
    
    // ========================================
    // USE CASES ДЛЯ ЗАДАЧ (TASKS)
    // ========================================
    factoryOf(::AddTaskUseCase)
    factoryOf(::GetAllTasksUseCase)
    factoryOf(::GetTaskByIdUseCase)
    factoryOf(::UpdateTaskUseCase)
    factoryOf(::CompleteTaskUseCase)
    factoryOf(::DeleteTaskUseCase)
    factoryOf(::GetTasksForTodayUseCase)
    
    // ========================================
    // USE CASES ДЛЯ ПОДЗАДАЧ (SUBTASKS)
    // ========================================
    factoryOf(::AddSubtaskUseCase)
    factoryOf(::UpdateSubtaskUseCase)
    factoryOf(::ToggleSubtaskUseCase)
    factoryOf(::DeleteSubtaskUseCase)
}


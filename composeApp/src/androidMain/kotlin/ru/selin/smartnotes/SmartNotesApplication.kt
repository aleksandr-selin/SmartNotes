package ru.selin.smartnotes

import android.app.Application
import ru.selin.smartnotes.di.PlatformKoinInitializer

/**
 * Application класс для Android
 * 
 * Отвечает за:
 * - Инициализацию Koin с Android-специфичными зависимостями
 * - Предоставление Context для DatabaseDriverFactory
 */
class SmartNotesApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Инициализация Koin для Android с Context
        PlatformKoinInitializer.initialize(this)
    }
}


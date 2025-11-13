package ru.selin.smartnotes.di

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

/**
 * Android реализация инициализации Koin
 * 
 * Инициализация происходит в SmartNotesApplication.onCreate()
 * с передачей Android Context.
 */
actual object PlatformKoinInitializer {
    
    private var isInitialized = false
    
    /**
     * Инициализирует Koin для Android
     * 
     * @param context Android Application Context
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        initKoin(platformModule = androidModule) {
            androidLogger(Level.ERROR)
            androidContext(context)
        }
        
        isInitialized = true
    }
    
    /**
     * Заглушка для общего интерфейса
     * На Android инициализация требует Context, поэтому используется перегрузка выше
     */
    actual fun initialize() {
        // Не используется на Android - вызывается initialize(context) из Application
    }
}


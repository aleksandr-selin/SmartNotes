package ru.selin.smartnotes.di

/**
 * iOS реализация инициализации Koin
 * 
 * Инициализация происходит автоматически при первом обращении к object.
 */
actual object PlatformKoinInitializer {
    
    private var isInitialized = false
    
    init {
        // Автоматическая инициализация при загрузке object
        doInitialize()
    }
    
    /**
     * Инициализирует Koin для iOS
     */
    actual fun initialize() {
        doInitialize()
    }
    
    private fun doInitialize() {
        if (isInitialized) return
        
        initKoin(platformModule = iosModule)
        isInitialized = true
    }
}


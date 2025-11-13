package ru.selin.smartnotes.di

/**
 * Platform-specific инициализация Koin
 * 
 * Каждая платформа (Android/iOS) предоставляет свою реализацию
 * для инициализации Koin с платформо-зависимыми модулями.
 */
expect object PlatformKoinInitializer {
    /**
     * Инициализирует Koin для конкретной платформы
     * 
     * Android: Вызывается из Application.onCreate()
     * iOS: Вызывается автоматически при первом обращении к Koin
     */
    fun initialize()
}


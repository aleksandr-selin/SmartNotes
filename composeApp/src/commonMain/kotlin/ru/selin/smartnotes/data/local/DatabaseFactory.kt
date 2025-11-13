package ru.selin.smartnotes.data.local

import ru.selin.smartnotes.database.NotesDatabase

/**
 * DatabaseFactory - фабрика для создания экземпляра NotesDatabase
 * 
 * Инкапсулирует логику создания базы данных через платформозависимый драйвер
 * 
 * Data Layer: Local Storage
 */
object DatabaseFactory {
    
    /**
     * Создаёт экземпляр NotesDatabase с использованием предоставленного драйвера
     * 
     * @param driverFactory Фабрика для создания SqlDriver (платформозависимая)
     * @return Экземпляр NotesDatabase готовый к использованию
     */
    fun createDatabase(driverFactory: DatabaseDriverFactory): NotesDatabase {
        val driver = driverFactory.createDriver()
        return NotesDatabase(driver)
    }
}


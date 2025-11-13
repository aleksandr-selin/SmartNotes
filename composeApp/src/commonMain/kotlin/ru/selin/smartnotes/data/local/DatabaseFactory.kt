package ru.selin.smartnotes.data.local

import ru.selin.smartnotes.database.NotesDatabase

/**
 * Создаёт экземпляр NotesDatabase с использованием предоставленного драйвера
 * 
 * Инкапсулирует логику создания базы данных через платформозависимый драйвер.
 * Используется в Koin DI для создания singleton экземпляра базы данных.
 * 
 * @param driverFactory Фабрика для создания SqlDriver (платформозависимая)
 * @return Экземпляр NotesDatabase готовый к использованию
 * 
 * Data Layer: Local Storage
 */
fun createDatabase(driverFactory: DatabaseDriverFactory): NotesDatabase {
    val driver = driverFactory.createDriver()
    return NotesDatabase(driver)
}


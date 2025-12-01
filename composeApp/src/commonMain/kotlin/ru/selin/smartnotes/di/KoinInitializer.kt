package ru.selin.smartnotes.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

/**
 * Инициализация Koin для всех платформ
 * 
 * @param platformModule - platform-specific модуль (Android/iOS)
 * @param appDeclaration - дополнительные настройки Koin (опционально)
 */
fun initKoin(
    platformModule: Module,
    appDeclaration: KoinAppDeclaration = {}
) = startKoin {
    appDeclaration()
    modules(
        platformModule,
        dataModule,
        domainModule,
        presentationModule
    )
}


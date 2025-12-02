package ru.selin.smartnotes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.selin.smartnotes.di.PlatformKoinInitializer
import ru.selin.smartnotes.presentation.navigation.RootScreen

/** Главная точка входа приложения */
@Composable
fun App() {
    // Инициализация Koin при первом запуске
    LaunchedEffect(Unit) {
        PlatformKoinInitializer.initialize()
    }
    
    MaterialTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            RootScreen()
        }
    }
}
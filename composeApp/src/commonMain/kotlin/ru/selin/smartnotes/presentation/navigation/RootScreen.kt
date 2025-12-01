package ru.selin.smartnotes.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import ru.selin.smartnotes.presentation.navigation.tabs.NotesTab
import ru.selin.smartnotes.presentation.navigation.tabs.TasksTab

/**
 * Корневой экран приложения с TabNavigator
 *
 * Содержит нижнюю навигацию между Notes и Tasks
 */
@Composable
fun RootScreen() {
    TabNavigator(NotesTab) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    TabNavigationItem(NotesTab)
                    TabNavigationItem(TasksTab)
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier.padding(paddingValues)
            ) {
                CurrentTab()
            }
        }
    }
}

/**
 * Элемент нижней навигации для таба
 */
@Composable
private fun RowScope.TabNavigationItem(tab: Tab) {
    val tabNavigator = LocalTabNavigator.current

    NavigationBarItem(
        selected = tabNavigator.current == tab,
        onClick = { tabNavigator.current = tab },
        icon = {
            tab.options.icon?.let { painter ->
                Icon(
                    painter = painter,
                    contentDescription = tab.options.title
                )
            }
        },
        label = { Text(tab.options.title) }
    )
}


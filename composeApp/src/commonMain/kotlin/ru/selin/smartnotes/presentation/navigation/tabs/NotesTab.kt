package ru.selin.smartnotes.presentation.navigation.tabs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import org.jetbrains.compose.resources.stringResource
import ru.selin.smartnotes.presentation.screens.notes.NotesListScreen
import smartnotes.composeapp.generated.resources.Res
import smartnotes.composeapp.generated.resources.tab_notes

/**
 * Таб для списка заметок
 * 
 * Использует Voyager Tab для навигации по нижнему меню
 */
object NotesTab : Tab {
    
    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Default.Description)
            val title = stringResource(Res.string.tab_notes)
            
            return remember(title) {
                TabOptions(
                    index = 0u,
                    title = title,
                    icon = icon
                )
            }
        }
    
    @Composable
    override fun Content() {
        Navigator(NotesListScreen())
    }
}

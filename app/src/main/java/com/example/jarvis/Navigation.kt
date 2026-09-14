package com.example.jarvis

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.jarvis.ui.main.JarvisViewModel
import com.example.jarvis.ui.main.MainScreen
import com.example.jarvis.ui.settings.SettingsScreen

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Main)
    val jarvisViewModel: JarvisViewModel = viewModel()
    val memories by jarvisViewModel.memories.collectAsState()

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Main> {
                MainScreen(
                    onNavigateToSettings = { backStack.add(Settings) },
                    viewModel = jarvisViewModel,
                    modifier = Modifier.safeDrawingPadding()
                )
            }
            entry<Settings> {
                SettingsScreen(
                    repository = jarvisViewModel.repository,
                    ttsEngine = jarvisViewModel.ttsEngine,
                    memories = memories,
                    onDeleteMemory = { id -> jarvisViewModel.deleteMemory(id) },
                    onClearMemories = { jarvisViewModel.clearMemories() },
                    onBack = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}

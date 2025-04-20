package ru.noxly.baumforms

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ru.noxly.baumforms.presentation.component.MainNavGraph
import ru.noxly.baumforms.presentation.component.Screen
import ru.noxly.baumforms.presentation.viewModel.MainStartupViewModel

@Composable
fun MainScreen(
    viewModel: MainStartupViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val items = listOf(Screen.Home, Screen.Sessions, Screen.Dev)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(Unit) {
        viewModel.tryRestoreServer()
    }

    Scaffold(
        bottomBar = {
            // Скрываем нижнюю панель на PermissionPage
            if (currentRoute != Screen.Permission.route) {
                NavigationBar {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        MainNavGraph(navController = navController, modifier = Modifier.padding(innerPadding))
    }
}

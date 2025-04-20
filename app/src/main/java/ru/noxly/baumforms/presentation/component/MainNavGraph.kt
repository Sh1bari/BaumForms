package ru.noxly.baumforms.presentation.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ru.noxly.baumforms.presentation.page.DevToolsPage
import ru.noxly.baumforms.presentation.page.ExcelManagementPage
import ru.noxly.baumforms.presentation.page.HomePage
import ru.noxly.baumforms.presentation.page.ManualReviewPage
import ru.noxly.baumforms.presentation.page.PermissionPage
import ru.noxly.baumforms.presentation.page.StudentsInSessionPage
import ru.noxly.baumforms.presentation.page.TestSessionDetailsPage
import ru.noxly.baumforms.presentation.page.TestSessionPage

@Composable
fun MainNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController, startDestination = Screen.Permission.route, modifier = modifier) {

        composable("session_detail/{sessionId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId")?.toIntOrNull()
            sessionId?.let {
                TestSessionDetailsPage(
                    sessionId = it,
                    onBack = { navController.popBackStack() },
                    onStudentsClick = { sid -> navController.navigate("students_in_session/$sid") },
                    onManageExcelClick = { sid -> navController.navigate("excel_manage/$sid") },
                    onReviewClick = { sid -> navController.navigate("manual_review/$sid") } // 👈
                )
            }
        }

        composable("students_in_session/{sessionId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId")?.toIntOrNull()
            sessionId?.let {
                StudentsInSessionPage(
                    sessionId = it,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.Permission.route) {
            PermissionPage(navController)
        }
        composable(Screen.Home.route) {
            HomePage()
        }
        composable(Screen.Dashboard.route) {
            Text("Dashboard Screen")
        }
        composable(Screen.Dev.route) {
            DevToolsPage()
        }
        composable("excel_manage/{sessionId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId")?.toIntOrNull()
            sessionId?.let {
                ExcelManagementPage(
                    sessionId = it,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable("manual_review/{sessionId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId")?.toIntOrNull()
            sessionId?.let {
                ManualReviewPage(
                    sessionId = it,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable(Screen.Sessions.route) {
            TestSessionPage(
                onSessionClick = { session ->
                    navController.navigate("session_detail/${session.id}")
                }
            )
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Info)
    object Dev : Screen("dev", "Dev", Icons.Default.Person)
    object Permission : Screen("permission", "Permissions", Icons.Default.Info)
    object Sessions : Screen("sessions", "Сессии", Icons.Default.Info)
}
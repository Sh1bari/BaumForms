package ru.noxly.baumforms.presentation.page

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import ru.noxly.baumforms.presentation.viewModel.TestSessionViewModel

@Composable
fun DevToolsPage(
    viewModel: TestSessionViewModel = hiltViewModel()
) {
    var showCreateStudentDialog by remember { mutableStateOf(false) }
    var showDeleteStudentsDialog by remember { mutableStateOf(false) }
    var showDeleteSessionDialog by remember { mutableStateOf(false) }

    var sessionId by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    var targetSessionId by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Dev Tools", style = MaterialTheme.typography.headlineSmall)

        Button(onClick = { showCreateStudentDialog = true }) {
            Text("Создать студента в сессии")
        }

        Button(
            onClick = { showDeleteStudentsDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Удалить всех студентов из сессии")
        }

        Button(
            onClick = { showDeleteSessionDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Удалить сессию")
        }
    }

    // === Диалог: Создание студента ===
    if (showCreateStudentDialog) {
        AlertDialog(
            onDismissRequest = { showCreateStudentDialog = false },
            title = { Text("Создание студента") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = sessionId, onValueChange = { sessionId = it }, label = { Text("ID сессии") })
                    OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("ФИО") })
                    OutlinedTextField(value = group, onValueChange = { group = it }, label = { Text("Группа") })
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val sid = sessionId.toIntOrNull()
                    if (sid != null && fullName.isNotBlank() && group.isNotBlank()) {
                        viewModel.createStudentInSession(sid, fullName, group, email.ifBlank { null })
                        sessionId = ""; fullName = ""; group = ""; email = ""
                        showCreateStudentDialog = false
                    }
                }) { Text("Создать") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateStudentDialog = false }) { Text("Отмена") }
            }
        )
    }

    // === Диалог: Удаление студентов ===
    if (showDeleteStudentsDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteStudentsDialog = false },
            title = { Text("Удалить всех студентов из сессии") },
            text = {
                OutlinedTextField(
                    value = targetSessionId,
                    onValueChange = { targetSessionId = it },
                    label = { Text("ID сессии") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val sid = targetSessionId.toIntOrNull()
                    if (sid != null) {
                        viewModel.deleteStudentsInSession(sid)
                        targetSessionId = ""
                        showDeleteStudentsDialog = false
                    }
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteStudentsDialog = false }) { Text("Отмена") }
            }
        )
    }

    // === Диалог: Удаление сессии ===
    if (showDeleteSessionDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSessionDialog = false },
            title = { Text("Удалить сессию") },
            text = {
                OutlinedTextField(
                    value = targetSessionId,
                    onValueChange = { targetSessionId = it },
                    label = { Text("ID сессии") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val sid = targetSessionId.toIntOrNull()
                    if (sid != null) {
                        scope.launch {
                            viewModel.deleteSessionById(sid)
                        }
                        targetSessionId = ""
                        showDeleteSessionDialog = false
                    }
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSessionDialog = false }) { Text("Отмена") }
            }
        )
    }
}
package ru.noxly.baumforms.presentation.page

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.noxly.baumforms.db.entity.TestSessionEntity
import ru.noxly.baumforms.presentation.component.TestSessionItem
import ru.noxly.baumforms.presentation.viewModel.TestSessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestSessionPage(
    viewModel: TestSessionViewModel = hiltViewModel(),
    onSessionClick: (TestSessionEntity) -> Unit
) {
    val sessions by viewModel.allSessions.observeAsState(emptyList())
    var showDialog by remember { mutableStateOf(false) }
    var sessionName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Тестирования") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Text("+")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            items(sessions) { session ->
                TestSessionItem(session = session) {
                    onSessionClick(session)
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    if (sessionName.isNotBlank()) {
                        viewModel.createSession(sessionName.trim())
                        sessionName = ""
                        showDialog = false
                    }
                }) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    sessionName = ""
                }) {
                    Text("Отмена")
                }
            },
            title = { Text("Новая сессия") },
            text = {
                OutlinedTextField(
                    value = sessionName,
                    onValueChange = { sessionName = it },
                    label = { Text("Название") },
                    singleLine = true
                )
            }
        )
    }
}
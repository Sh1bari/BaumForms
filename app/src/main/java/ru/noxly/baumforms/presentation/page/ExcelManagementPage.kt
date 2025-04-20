package ru.noxly.baumforms.presentation.page

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.noxly.baumforms.db.entity.TestSessionStatus
import ru.noxly.baumforms.presentation.component.ExcelFilePreview
import ru.noxly.baumforms.presentation.component.ExcelQuestionPreview
import ru.noxly.baumforms.presentation.viewModel.ExcelManagementViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelManagementPage(
    sessionId: Int,
    viewModel: ExcelManagementViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val session by viewModel.session.collectAsState()
    val error by viewModel.error.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPreview by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.attachFile(context, sessionId, it)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadSession(sessionId)
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Excel-файл сессии") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            session?.let { s ->
                val isEditable = s.status == TestSessionStatus.CREATED

                Text("Сессия: ${s.name}", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                val filePath = s.filePath
                if (!filePath.isNullOrBlank()) {
                    val file = File(filePath)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExcelFilePreview(
                            onOpen = { viewModel.openFile(context, file.absolutePath) },
                            onDelete = { viewModel.removeFile(sessionId) },
                            showDeleteIcon = isEditable
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Файл: ${file.name}", style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(4.dp))

                            if (isEditable) {
                                Button(onClick = { filePicker.launch("*/*") }) {
                                    Text("Заменить")
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (!showPreview) {
                                viewModel.loadQuestionsFromFile(file)
                            }
                            showPreview = !showPreview
                        }
                    ) {
                        Text(if (showPreview) "Скрыть предпросмотр" else "Показать предпросмотр теста")
                    }

                    if (showPreview && questions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Предпросмотр теста", style = MaterialTheme.typography.titleMedium)

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            questions.forEach { q ->
                                ExcelQuestionPreview(question = q)
                            }
                        }
                    }

                } else {
                    Text("Файл не прикреплён", style = MaterialTheme.typography.bodyLarge)

                    if (isEditable) {
                        Button(onClick = { filePicker.launch("*/*") }) {
                            Text("Прикрепить файл")
                        }
                    }
                }
            } ?: Text("Загрузка...")
        }
    }
}

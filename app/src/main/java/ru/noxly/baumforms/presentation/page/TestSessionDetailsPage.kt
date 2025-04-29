package ru.noxly.baumforms.presentation.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.noxly.baumforms.db.entity.TestSessionStatus
import ru.noxly.baumforms.presentation.component.ExcelFilePreview
import ru.noxly.baumforms.presentation.viewModel.TestSessionViewModel
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestSessionDetailsPage(
    sessionId: Int,
    viewModel: TestSessionViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onStudentsClick: (Int) -> Unit,
    onManageExcelClick: (Int) -> Unit,
    onReviewClick: (Int) -> Unit
) {
    val session by viewModel.getSessionById(sessionId).observeAsState()
    val students by viewModel.getStudentsForSession(sessionId).observeAsState(emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val error by viewModel.errorMessage
    val exportedFilePath by viewModel.exportedFilePath
    val context = LocalContext.current

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детали тестирования") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        session?.let { s ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Section(title = "Общая информация") {
                    Text("Название: ${s.name}")
                    Text(
                        text = "Статус: ${
                            when (s.status) {
                                TestSessionStatus.CREATED -> "Не начат"
                                TestSessionStatus.STARTED -> "В процессе"
                                TestSessionStatus.FINISHED -> "Завершён"
                            }
                        }"
                    )
                    Text(
                        "Дата создания: ${
                            DateFormat.getDateTimeInstance().format(Date(s.createdAt))
                        }"
                    )
                }

                Section(title = "Файл теста") {
                    s.filePath?.let {
                        Text("Файл: $it", style = MaterialTheme.typography.bodyMedium)
                    } ?: Text("Файл не загружен")

                    Button(onClick = { onManageExcelClick(s.id) }) {
                        Text("Управление Excel-файлом")
                    }
                }

                if (s.status != TestSessionStatus.CREATED) {
                    Section(title = "Студенты") {
                        Text("Студентов привязано: ${students.size}")
                        Button(onClick = { onStudentsClick(s.id) }) {
                            Text("Просмотр студентов")
                        }
                    }
                }

                if (s.status == TestSessionStatus.FINISHED) {
                    Section(title = "Оценивание") {
                        Button(onClick = { onReviewClick(s.id) }) {
                            Text("Перейти к оцениванию")
                        }

                        Button(onClick = { viewModel.exportFullResults(s.id) }) {
                            Text("Скачать результаты")
                        }

                        if (!exportedFilePath.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Результаты тестирования:",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )

                                ExcelFilePreview(
                                    onOpen = { viewModel.openGeneratedExcelFile(context) },
                                    onDelete = {},
                                    showDeleteIcon = false
                                )
                            }
                        }

                    }
                }

                if (s.status == TestSessionStatus.CREATED) {
                    Button(onClick = { viewModel.startSession(s.id) }) {
                        Text("Старт")
                    }
                }

                if (s.status == TestSessionStatus.STARTED) {
                    Button(onClick = { viewModel.stopSession(s.id) }) {
                        Text("Остановить")
                    }
                }
            }
        } ?: Text("Загрузка...", modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

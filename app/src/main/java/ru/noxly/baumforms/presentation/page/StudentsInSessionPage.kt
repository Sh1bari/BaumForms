package ru.noxly.baumforms.presentation.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.noxly.baumforms.db.entity.TestSessionStatus
import ru.noxly.baumforms.helper.TestResultEvaluator
import ru.noxly.baumforms.model.StudentAnswer
import ru.noxly.baumforms.model.TestQuestion
import ru.noxly.baumforms.presentation.viewModel.TestSessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsInSessionPage(
    sessionId: Int,
    viewModel: TestSessionViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val students by viewModel.getStudentsForSession(sessionId).observeAsState(emptyList())
    val session by viewModel.getSessionById(sessionId).observeAsState()
    val gson = remember { Gson() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Студенты сессии") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(students) { student ->
                val hasAnswers = !student.answersJson.isNullOrBlank()
                val manualScoresMap: Map<Pair<Int, String>, Int> = try {
                    session?.manualGradesJson?.let {
                        val grades: List<ru.noxly.baumforms.model.ManualGrade> = gson.fromJson(
                            it,
                            object : TypeToken<List<ru.noxly.baumforms.model.ManualGrade>>() {}.type
                        )
                        grades.associate { grade -> (grade.questionIndex to grade.answerText.trim()) to grade.score }
                    } ?: emptyMap()
                } catch (e: Exception) {
                    emptyMap()
                }

                val (score, pendingReview, maxScore) = try {
                    TestResultEvaluator.evaluate(
                        session!!.questionsJson!!,
                        student.answersJson!!,
                        manualScoresMap
                    )
                } catch (e: Exception) {
                    Triple(0, 0, 0)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasAnswers)
                            MaterialTheme.colorScheme.surfaceVariant
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(student.fullName, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Группа: ${student.group}", style = MaterialTheme.typography.bodyMedium)
                        student.mail?.let {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Почта: $it", style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        if (hasAnswers) {
                            Text(
                                text = "✅ Тест пройден",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Баллы: $score / $maxScore", style = MaterialTheme.typography.bodyMedium)

                            if (pendingReview > 0) {
                                Text(
                                    "⏳ Требует проверки: $pendingReview ответ(ов)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        } else {
                            Text(
                                text = if (session?.status == TestSessionStatus.FINISHED) "❌ Не сдал" else "🕓 Ожидание ответов",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (session?.status == TestSessionStatus.FINISHED)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (students.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Студенты не найдены", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
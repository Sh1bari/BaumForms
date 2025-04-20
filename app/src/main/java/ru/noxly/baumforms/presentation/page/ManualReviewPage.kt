package ru.noxly.baumforms.presentation.page

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.noxly.baumforms.model.AnswerType
import ru.noxly.baumforms.model.ManualGrade
import ru.noxly.baumforms.model.StudentAnswer
import ru.noxly.baumforms.model.TestQuestion
import ru.noxly.baumforms.presentation.viewModel.TestSessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualReviewPage(
    sessionId: Int,
    viewModel: TestSessionViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val session by viewModel.getSessionById(sessionId).observeAsState()
    val students by viewModel.getStudentsForSession(sessionId).observeAsState(emptyList())
    val gson = remember { Gson() }

    if (session == null || students.isEmpty()) {
        Text("Загрузка...", modifier = Modifier.padding(16.dp))
        return
    }

    val questions = remember {
        gson.fromJson(session!!.questionsJson, Array<TestQuestion>::class.java).toList()
    }

    val alreadyGraded = remember {
        session!!.manualGradesJson?.let {
            gson.fromJson<List<ManualGrade>>(it, object : TypeToken<List<ManualGrade>>() {}.type)
        } ?: emptyList()
    }

    val gradedMap = remember(alreadyGraded) {
        alreadyGraded.associateBy { it.questionIndex to it.answerText.trim().lowercase() }
    }

    val studentAnswers = remember(students) {
        students.flatMap { student ->
            try {
                if (!student.answersJson.isNullOrBlank()) {
                    gson.fromJson<List<StudentAnswer>>(
                        student.answersJson,
                        object : TypeToken<List<StudentAnswer>>() {}.type
                    )
                } else emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }


    var currentQuestionIndex by remember { mutableStateOf(0) }

    val nextQuestionIndex: Int? = remember(currentQuestionIndex, gradedMap, studentAnswers) {
        val start = currentQuestionIndex
        val remaining = questions.withIndex()
            .drop(start)
            .firstOrNull { (i, q) ->
                (q.type == AnswerType.FREE_FORM || q.type == AnswerType.LECTURE) &&
                        studentAnswers.any {
                            it.questionIndex == i &&
                                    !it.answerText.isNullOrBlank() &&
                                    !gradedMap.containsKey(i to it.answerText.trim().lowercase())
                        }
            }?.index
        remaining
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Оценка ответов") },
                navigationIcon = {
                    IconButton(onClick = {
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            if (nextQuestionIndex != null) {
                val q = questions[nextQuestionIndex]

                ManualReviewQuestionBlock(
                    questionIndex = nextQuestionIndex,
                    question = q,
                    allStudentAnswers = studentAnswers,
                    alreadyGraded = gradedMap.mapValues { it.value.score } // <-- преобразование
                ) { newGrades ->
                    viewModel.saveManualGrades(sessionId, newGrades)
                    currentQuestionIndex = nextQuestionIndex + 1
                }

            } else {
                Text(
                    "Все вопросы проверены.",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun ManualReviewQuestionBlock(
    questionIndex: Int,
    question: TestQuestion,
    allStudentAnswers: List<StudentAnswer>,
    alreadyGraded: Map<Pair<Int, String>, Int>,
    onSubmit: (List<ManualGrade>) -> Unit
) {
    val notYetGradedAnswers = remember(questionIndex, allStudentAnswers, alreadyGraded) {
        allStudentAnswers
            .filter { it.questionIndex == questionIndex && !it.answerText.isNullOrBlank() }
            .distinctBy { it.answerText!!.trim().lowercase() }
            .filterNot { alreadyGraded.containsKey(questionIndex to it.answerText!!.trim().lowercase()) }
            .map { it.answerText!!.trim() }
    }

    val grades = remember(questionIndex) {
        mutableStateMapOf<String, Float>().apply {
            notYetGradedAnswers.forEach { put(it, 0f) }
        }
    }

    if (notYetGradedAnswers.isEmpty()) {
        Text("Все ответы на вопрос \"${question.question}\" уже оценены.")
        return
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Вопрос: ${question.question}", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text("Максимальный балл: ${question.maxScore}", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        if (!question.answer.isNullOrBlank()) {
            Text("Ожидаемый ответ: ${question.answer}", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(12.dp))
        }

        grades.forEach { (answer, score) ->
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text("Ответ: $answer", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = score,
                    onValueChange = { grades[answer] = it },
                    valueRange = 0f..question.maxScore.toFloat(),
                    steps = (question.maxScore * 2) - 1 // по 0.5
                )
                Text("Оценка: ${"%.1f".format(score)}")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val result = grades.map {
                    ManualGrade(questionIndex, it.key.trim().lowercase(), it.value.toInt())
                }
                onSubmit(result)
            }
        ) {
            Text("Далее")
        }
    }
}
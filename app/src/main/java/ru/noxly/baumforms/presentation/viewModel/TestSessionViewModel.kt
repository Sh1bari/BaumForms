package ru.noxly.baumforms.presentation.viewModel

import android.content.Context
import android.os.Environment
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import ru.noxly.baumforms.db.entity.StudentEntity
import ru.noxly.baumforms.db.entity.TestSessionEntity
import ru.noxly.baumforms.helper.TestResultEvaluator
import ru.noxly.baumforms.model.ManualGrade
import ru.noxly.baumforms.model.TestQuestion
import ru.noxly.baumforms.service.ExcelService
import ru.noxly.baumforms.service.StudentService
import ru.noxly.baumforms.service.TestSessionService
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class TestSessionViewModel @Inject constructor(
    private val sessionService: TestSessionService,
    private val studentService: StudentService,
    private val excelService: ExcelService
) : ViewModel() {

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> get() = _errorMessage

    private val _exportedFilePath = mutableStateOf<String?>(null)
    val exportedFilePath: State<String?> get() = _exportedFilePath

    val allSessions: LiveData<List<TestSessionEntity>> =
        sessionService.getAllSessions().asLiveData()

    fun getSessionById(id: Int): LiveData<TestSessionEntity?> =
        sessionService.getSessionById(id).asLiveData()

    fun createSession(name: String) {
        viewModelScope.launch {
            sessionService.createSession(TestSessionEntity(name = name))
        }
    }

    fun openGeneratedExcelFile(context: Context) {
        val filePath = _exportedFilePath.value
        if (!filePath.isNullOrBlank()) {
            excelService.openExcelFile(context, filePath)
        } else {
            _errorMessage.value = "Файл не найден"
        }
    }

    fun exportResultsToExcel(sessionId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val session = sessionService.getSessionById(sessionId).firstOrNull()
                val students = studentService.getStudentsBySession(sessionId).firstOrNull().orEmpty()

                if (session == null || session.questionsJson.isNullOrBlank()) {
                    _errorMessage.value = "Сессия не найдена или не содержит вопросов"
                    return@launch
                }

                val questions = Gson().fromJson<List<TestQuestion>>(
                    session.questionsJson,
                    object : TypeToken<List<TestQuestion>>() {}.type
                )

                val manualGrades: List<ManualGrade> = session.manualGradesJson?.let {
                    Gson().fromJson(it, object : TypeToken<List<ManualGrade>>() {}.type)
                } ?: emptyList()

                val manualMap = manualGrades.associate {
                    (it.questionIndex to it.answerText.trim().lowercase()) to it.score
                }

                for (student in students) {
                    val (_, pending, _) = TestResultEvaluator.evaluate(
                        session.questionsJson,
                        student.answersJson ?: "[]",
                        manualMap
                    )
                    if (pending > 0) {
                        _errorMessage.value = "Не все ответы проверены. Завершите ручную оценку."
                        return@launch
                    }
                }

                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Результаты")
                val header = sheet.createRow(0)

                header.createCell(0).setCellValue("Группа")
                header.createCell(1).setCellValue("Студент")
                header.createCell(2).setCellValue("Балл")

                students.forEachIndexed { i, student ->
                    val row = sheet.createRow(i + 1)
                    row.createCell(0).setCellValue(student.group)
                    row.createCell(1).setCellValue(student.fullName)

                    val (score, _, _) = TestResultEvaluator.evaluate(
                        session.questionsJson,
                        student.answersJson ?: "[]",
                        manualMap
                    )
                    row.createCell(2).setCellValue(score.toDouble())
                }

                // 👉 Получаем ту же директорию, куда сохранился исходный Excel-файл
                val originalDir = session.filePath?.let { File(it).parentFile }
                    ?: File(Environment.getExternalStorageDirectory(), "Download")

                val safeSessionName = session.name.replace("\\W+".toRegex(), "_")
                val randomPart = (1000..9999).random()
                val resultFileName = "Result_${safeSessionName}_$randomPart.xlsx"

                val file = File(originalDir, resultFileName).apply {
                    parentFile?.mkdirs()
                }

                FileOutputStream(file).use { workbook.write(it) }
                workbook.close()

                _exportedFilePath.value = file.absolutePath
                //_errorMessage.value = "Файл создан: ${file.absolutePath}"

            } catch (e: Exception) {
                _errorMessage.value = "Ошибка при экспорте: ${e.message}"
            }
        }
    }

    fun getStudentsForSession(sessionId: Int) =
        studentService.getStudentsBySession(sessionId).asLiveData()

    fun startSession(sessionId: Int) {
        viewModelScope.launch {
            try {
                val session = sessionService.getSessionById(sessionId).firstOrNull()
                    ?: throw IllegalStateException("Сессия не найдена")

                if (session.filePath.isNullOrBlank()) {
                    throw IllegalStateException("К сессии не прикреплён Excel-файл!")
                }

                // ✅ Парсим Excel-файл
                val file = File(session.filePath)
                val questions = excelService.parseExcelFile(file)
                val questionsJson = Gson().toJson(questions)

                println("Parsed Questions JSON: $questionsJson")

                // ✅ Обновляем сессию с questionsJson
                val updatedSession = session.copy(questionsJson = questionsJson)
                sessionService.updateSession(updatedSession)

                // ✅ Теперь запускаем
                sessionService.startSession(sessionId)


            } catch (e: IllegalStateException) {
                _errorMessage.value = e.message
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка запуска: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun stopSession(sessionId: Int) {
        viewModelScope.launch {
            sessionService.stopSession(sessionId)
        }
    }

    fun saveManualGrades(sessionId: Int, newGrades: List<ManualGrade>) {
        viewModelScope.launch {
            val session = sessionService.getSessionById(sessionId).firstOrNull() ?: return@launch
            val gson = Gson()

            val existingGrades: List<ManualGrade> = session.manualGradesJson?.takeIf { it.isNotBlank() }?.let {
                gson.fromJson(it, object : TypeToken<List<ManualGrade>>() {}.type)
            } ?: emptyList()

            // Объединяем старые и новые оценки, избегая дубликатов
            val combinedGrades = (existingGrades + newGrades)
                .distinctBy { it.questionIndex to it.answerText.trim().lowercase() }

            val updatedSession = session.copy(
                manualGradesJson = gson.toJson(combinedGrades)
            )
            sessionService.updateSession(updatedSession)
        }
    }

    fun createStudentInSession(sessionId: Int, fullName: String, group: String, mail: String?) {
        viewModelScope.launch {
            studentService.addStudent(
                StudentEntity(
                    fullName = fullName,
                    group = group,
                    mail = mail,
                    sessionId = sessionId
                )
            )
        }
    }

    fun deleteStudentsInSession(sessionId: Int) {
        viewModelScope.launch {
            studentService.deleteStudentsBySession(sessionId)
        }
    }

    fun deleteSessionById(sessionId: Int) {
        viewModelScope.launch {
            val session = sessionService.getSessionById(sessionId).firstOrNull()
            if (session != null) {
                sessionService.deleteSession(session)
            }
        }
    }
}
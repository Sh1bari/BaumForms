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
import org.apache.poi.ss.util.CellReference
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import ru.noxly.baumforms.db.entity.StudentEntity
import ru.noxly.baumforms.db.entity.TestSessionEntity
import ru.noxly.baumforms.helper.TestResultEvaluator
import ru.noxly.baumforms.model.AnswerType
import ru.noxly.baumforms.model.ManualGrade
import ru.noxly.baumforms.model.StudentAnswer
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

    fun exportFullResults(sessionId: Int) {
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

                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Результаты")

                // Заголовки
                val headerRow1 = sheet.createRow(0)
                val headerRow2 = sheet.createRow(1)
                headerRow1.createCell(0).setCellValue("ФИО")
                headerRow1.createCell(1).setCellValue("Группа")
                headerRow2.createCell(0).setCellValue("Ответ")
                headerRow2.createCell(1).setCellValue("Правильный ответ")

                questions.forEachIndexed { index, q ->
                    val col = 2 + index * 2
                    val optionsText = if (q.type == AnswerType.SINGLE_CHOICE || q.type == AnswerType.MULTIPLE_CHOICE)
                        " [${q.options.joinToString("; ")}]" else ""
                    headerRow1.createCell(col).setCellValue("Вопрос ${index + 1}: ${q.question}$optionsText")
                    val correct = when (q.type) {
                        AnswerType.SINGLE_CHOICE, AnswerType.MULTIPLE_CHOICE -> q.correctAnswers.joinToString(", ")
                        else -> q.answer ?: ""
                    }
                    headerRow2.createCell(col).setCellValue(correct)
                    headerRow1.createCell(col + 1).setCellValue("Тип: ${q.type.name}")
                    headerRow2.createCell(col + 1).setCellValue("Макс. балл: ${q.maxScore}")
                    sheet.setColumnWidth(col, 8000)
                    sheet.setColumnWidth(col + 1, 4000)
                }

                val sumColIndex = 2 + questions.size * 2
                headerRow1.createCell(sumColIndex).setCellValue("Сумма")
                headerRow2.createCell(sumColIndex).setCellValue("")

                students.forEachIndexed { studentIdx, student ->
                    val row = sheet.createRow(studentIdx + 2)
                    row.createCell(0).setCellValue(student.fullName)
                    row.createCell(1).setCellValue(student.group)

                    val answers = if (!student.answersJson.isNullOrBlank()) {
                        Gson().fromJson<List<StudentAnswer>>(
                            student.answersJson,
                            object : TypeToken<List<StudentAnswer>>() {}.type
                        )
                    } else emptyList()

                    val answerMap = answers.associateBy { it.questionIndex }

                    questions.forEachIndexed { qIdx, question ->
                        val answer = answerMap[qIdx]
                        val col = 2 + qIdx * 2
                        val responseText = when (question.type) {
                            AnswerType.LECTURE, AnswerType.FREE_FORM -> answer?.answerText ?: ""
                            else -> answer?.selectedOptions?.joinToString(", ") ?: ""
                        }

                        val score = when (question.type) {
                            AnswerType.SINGLE_CHOICE, AnswerType.MULTIPLE_CHOICE -> {
                                val correctSet = question.correctAnswers.toSet()
                                val selectedSet = answer?.selectedOptions?.toSet() ?: emptySet()
                                val correctMatches = selectedSet.intersect(correctSet).size
                                val extraSelections = selectedSet.subtract(correctSet).size
                                val totalSelections = correctMatches + extraSelections
                                if (totalSelections > 0 && correctSet.isNotEmpty()) {
                                    kotlin.math.ceil(correctMatches.toDouble() / totalSelections * question.maxScore).toInt()
                                } else 0
                            }

                            else -> {
                                val key = qIdx to (answer?.answerText?.trim()?.lowercase().orEmpty())
                                manualMap[key] ?: 0
                            }
                        }

                        row.createCell(col).setCellValue(responseText)
                        row.createCell(col + 1).setCellValue(score.toDouble())
                    }

                    // Формула суммы баллов только по колонкам с баллами (начиная с D, через одну)
                    val scoreColumns = (0 until questions.size).map { qIdx ->
                        CellReference.convertNumToColString(3 + qIdx * 2)
                    }
                    val excelRowNum = row.rowNum + 1
                    val formula = scoreColumns.joinToString("+") { "$it$excelRowNum" }
                    row.createCell(sumColIndex).cellFormula = formula
                }

                // Второй лист - "Итог"
                val summarySheet = workbook.createSheet("Итог")
                val summaryHeader = summarySheet.createRow(0)
                summaryHeader.createCell(0).setCellValue("Группа")
                summaryHeader.createCell(1).setCellValue("ФИО")
                summaryHeader.createCell(2).setCellValue("Сумма баллов")

                students.forEachIndexed { index, student ->
                    val row = summarySheet.createRow(index + 1)
                    row.createCell(0).setCellValue(student.group)
                    row.createCell(1).setCellValue(student.fullName)

                    // Ссылка на итоговую колонку с листа "Результаты"
                    val formula = "Результаты!${CellReference.convertNumToColString(sumColIndex)}${index + 3}"
                    row.createCell(2).cellFormula = formula
                }

                val outputDir = session.filePath?.let { File(it).parentFile }
                    ?: File(Environment.getExternalStorageDirectory(), "Download")
                val fileName = "FullResults_${session.name.replace("\\W+".toRegex(), "_")}_${System.currentTimeMillis()}.xlsx"
                val file = File(outputDir, fileName)

                FileOutputStream(file).use { workbook.write(it) }
                workbook.close()

                _exportedFilePath.value = file.absolutePath

            } catch (e: Exception) {
                _errorMessage.value = "Ошибка экспорта: ${e.message}"
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
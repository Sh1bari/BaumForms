package ru.noxly.baumforms.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import ru.noxly.baumforms.db.entity.TestSessionEntity
import ru.noxly.baumforms.model.AnswerType
import ru.noxly.baumforms.model.TestQuestion
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExcelService @Inject constructor(
    private val sessionService: TestSessionService
) {

    suspend fun getSession(sessionId: Int): TestSessionEntity? {
        return sessionService.getSessionById(sessionId).firstOrNull()
    }

    suspend fun saveExcelFile(context: Context, sessionId: Int, uri: Uri) {
        withContext(Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(uri)
            val fileName = getFileName(context, uri) ?: "excel_${System.currentTimeMillis()}.xlsx"

            val dir = context.getExternalFilesDir("excel")
            if (dir?.exists() == false) dir.mkdirs()

            val file = File(dir, fileName)

            inputStream?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }

            sessionService.attachExcelFile(sessionId, file.absolutePath)
        }
    }

    fun parseExcelFile(file: File): List<TestQuestion> {
        val questions = mutableListOf<TestQuestion>()
        val workbook = WorkbookFactory.create(file)
        val sheet = workbook.getSheetAt(0)

        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex)
            if (row != null) {
                val rawQuestionText = getCellString(row.getCell(0)) ?: continue
                val rawType = getCellString(row.getCell(1)) ?: ""
                val answer = getCellString(row.getCell(2))
                val score = row.getCell(3)?.numericCellValue?.toInt() ?: 0

                val type = AnswerType.fromRaw(rawType)
                val (questionText, options) = extractOptionsAndCleanQuestion(rawQuestionText)

                val correctAnswers = when (type) {
                    AnswerType.SINGLE_CHOICE, AnswerType.MULTIPLE_CHOICE -> {
                        answer
                            ?.split(",", ";")
                            ?.mapNotNull {
                                it.trim()
                                    .removeSuffix(".0")
                                    .toIntOrNull()
                            } ?: emptyList()
                    }

                    else -> emptyList()
                }

                questions.add(
                    TestQuestion(
                        question = questionText,
                        type = type,
                        answer = if (type == AnswerType.FREE_FORM || type == AnswerType.LECTURE) answer else null,
                        options = options,
                        correctAnswers = correctAnswers,
                        maxScore = score
                    )
                )
            }
        }

        workbook.close()
        return questions
    }


    private fun extractOptionsAndCleanQuestion(raw: String): Pair<String, List<String>> {
        val regex = Regex("""(?<=\n|^)(\d{1,2})[).:\- ]+(.+?)(?=\n\d|$)""", RegexOption.MULTILINE)
        val matches = regex.findAll(raw).toList()
        val options = matches.map { "${it.groupValues[1]}. ${it.groupValues[2].trim()}" }

        val cleanedQuestion = matches.fold(raw) { acc, match ->
            acc.replace(match.value, "").trim()
        }.replace(Regex("""\n{2,}"""), "\n").trim() // Убираем лишние переносы строк

        return cleanedQuestion to options
    }


    private fun getCellString(cell: Cell?): String? {
        return when (cell?.cellType) {
            CellType.STRING -> cell.stringCellValue.trim()
            CellType.NUMERIC -> cell.numericCellValue.toString().trim()
            CellType.BOOLEAN -> cell.booleanCellValue.toString().trim()
            CellType.FORMULA -> cell.toString().trim()
            else -> null
        }
    }

    suspend fun deleteExcelFile(sessionId: Int) {
        sessionService.attachExcelFile(sessionId, null)
    }

    fun openExcelFile(context: Context, filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Открыть Excel-файл"))
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            it.getString(nameIndex)
        }
    }
}
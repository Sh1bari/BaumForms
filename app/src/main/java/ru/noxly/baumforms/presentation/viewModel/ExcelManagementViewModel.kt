package ru.noxly.baumforms.presentation.viewModel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.noxly.baumforms.db.entity.TestSessionEntity
import ru.noxly.baumforms.model.TestQuestion
import ru.noxly.baumforms.service.ExcelService
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ExcelManagementViewModel @Inject constructor(
    private val excelService: ExcelService
) : ViewModel() {

    private val _session = MutableStateFlow<TestSessionEntity?>(null)
    val session: StateFlow<TestSessionEntity?> = _session.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _questions = MutableStateFlow<List<TestQuestion>>(emptyList())
    val questions: StateFlow<List<TestQuestion>> = _questions.asStateFlow()

    fun loadQuestionsFromFile(file: File) {
        viewModelScope.launch {
            try {
                val parsedQuestions = excelService.parseExcelFile(file)
                _questions.value = parsedQuestions
            } catch (e: Exception) {
                _error.value = "Ошибка чтения Excel: ${e.message}"
            }
        }
    }

    fun loadSession(sessionId: Int) {
        viewModelScope.launch {
            try {
                val data = excelService.getSession(sessionId)
                _session.value = data
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки сессии: ${e.message}"
            }
        }
    }

    fun attachFile(context: Context, sessionId: Int, uri: Uri) {
        viewModelScope.launch {
            try {
                excelService.saveExcelFile(context, sessionId, uri)
                loadSession(sessionId)
            } catch (e: Exception) {
                _error.value = "Ошибка при сохранении файла: ${e.message}"
            }
        }
    }

    fun removeFile(sessionId: Int) {
        viewModelScope.launch {
            try {
                excelService.deleteExcelFile(sessionId)
                loadSession(sessionId)
            } catch (e: Exception) {
                _error.value = "Ошибка при удалении файла: ${e.message}"
            }
        }
    }

    fun openFile(context: Context, path: String) {
        excelService.openExcelFile(context, path)
    }

    fun clearError() {
        _error.value = null
    }
}
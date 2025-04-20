package ru.noxly.baumforms.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.noxly.baumforms.helper.SessionStateManager
import ru.noxly.baumforms.service.TestSessionService
import javax.inject.Inject

@HiltViewModel
class MainStartupViewModel @Inject constructor(
    private val sessionStateManager: SessionStateManager,
    private val sessionService: TestSessionService
) : ViewModel() {

    fun tryRestoreServer() {
        val sessionId = sessionStateManager.getActiveSession()
        if (sessionId != null) {
            viewModelScope.launch {
                val session = sessionService.getSessionById(sessionId).first()
                if (session?.status == ru.noxly.baumforms.db.entity.TestSessionStatus.STARTED) {
                    sessionService.restartServer()
                }
            }
        }
    }
}
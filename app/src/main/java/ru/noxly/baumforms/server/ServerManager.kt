package ru.noxly.baumforms.server

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.noxly.baumforms.helper.ServerEventBus
import ru.noxly.baumforms.helper.SessionStateManager
import ru.noxly.baumforms.service.StudentService
import ru.noxly.baumforms.service.TestSessionService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerManager @Inject constructor(
    private val testSessionService: TestSessionService,
    private val studentService: StudentService,
    private val serverEventBus: ServerEventBus,
    private val sessionStateManager: SessionStateManager
) {

    private var server: LocalHttpServer? = null
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    init {
        CoroutineScope(Dispatchers.IO).launch {
            serverEventBus.events.collectLatest { event ->
                println("🚀 ServerManager получил событие: ${event}")
                when (event) {
                    is ServerEvent.Start -> start()
                    is ServerEvent.Stop -> stop()
                    is ServerEvent.Restart -> restart()
                }
            }
        }
    }

    private fun start() {
        if (server == null) {
            val newServer = LocalHttpServer(testSessionService, studentService, sessionStateManager)
            newServer.start()
            server = newServer
            _isRunning.value = true
        }
    }

    private fun stop() {
        server?.stop()
        server = null
        _isRunning.value = false
    }

    private fun restart() {
        stop()
        start()
    }
}

sealed class ServerEvent {
    object Start : ServerEvent()
    object Stop : ServerEvent()
    object Restart : ServerEvent()
}
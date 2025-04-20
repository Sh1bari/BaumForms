package ru.noxly.baumforms.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import ru.noxly.baumforms.db.dao.TestSessionDao
import ru.noxly.baumforms.db.entity.TestSessionEntity
import ru.noxly.baumforms.db.entity.TestSessionStatus
import ru.noxly.baumforms.helper.ServerEventBus
import ru.noxly.baumforms.helper.SessionStateManager
import ru.noxly.baumforms.server.ServerEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestSessionService @Inject constructor(
    private val sessionDao: TestSessionDao,
    private val sessionStateManager: SessionStateManager,
    private val serverEventBus: ServerEventBus
) {

    fun getAllSessions(): Flow<List<TestSessionEntity>> =
        sessionDao.getAll()

    suspend fun updateSession(session: TestSessionEntity) {
        sessionDao.insert(session)
    }

    suspend fun createSession(session: TestSessionEntity): Long =
        sessionDao.insert(session)

    suspend fun deleteSession(session: TestSessionEntity) =
        sessionDao.delete(session)

    fun getSessionById(id: Int): Flow<TestSessionEntity?> = sessionDao.getById(id)

    suspend fun startSession(sessionId: Int) {
        val runningSessions = sessionDao.getSessionsByStatus(TestSessionStatus.STARTED)

        val isAlreadyRunning = runningSessions.any { it.id != sessionId }
        if (isAlreadyRunning) {
            throw IllegalStateException("Другая сессия уже запущена!")
        }

        val session = sessionDao.getById(sessionId).first()
            ?: throw IllegalStateException("Сессия не найдена")

        if (session.filePath.isNullOrBlank()) {
            throw IllegalStateException("К сессии не прикреплён Excel-файл!")
        }

        if (session.status == TestSessionStatus.CREATED) {
            sessionDao.insert(session.copy(status = TestSessionStatus.STARTED))
            sessionStateManager.setActiveSession(sessionId)
            serverEventBus.send(ServerEvent.Start)
        }
    }

    suspend fun stopSession(sessionId: Int) {
        sessionDao.updateStatus(sessionId, TestSessionStatus.FINISHED)
        sessionStateManager.clearActiveSession()
        serverEventBus.send(ServerEvent.Stop)
    }

    suspend fun restartServer() {
        serverEventBus.send(ServerEvent.Restart)
    }

    suspend fun attachExcelFile(sessionId: Int, filePath: String?) {
        val session = sessionDao.getById(sessionId).first()
        if (session != null) {
            val updated = session.copy(filePath = filePath)
            sessionDao.insert(updated)
        }
    }
}
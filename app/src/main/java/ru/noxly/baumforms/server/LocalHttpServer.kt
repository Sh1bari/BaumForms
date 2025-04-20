package ru.noxly.baumforms.server

import android.util.Log
import com.google.gson.Gson
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.flow.first
import ru.noxly.baumforms.db.entity.StudentEntity
import ru.noxly.baumforms.db.entity.TestSessionStatus
import ru.noxly.baumforms.helper.SessionStateManager
import ru.noxly.baumforms.server.handler.StudentRegistrationHandler
import ru.noxly.baumforms.server.handler.TestPageHandler
import ru.noxly.baumforms.server.handler.TestSubmissionHandler
import ru.noxly.baumforms.server.handler.serveRegistrationPage
import ru.noxly.baumforms.service.StudentService
import ru.noxly.baumforms.service.TestSessionService

class LocalHttpServer(
    sessionService: TestSessionService,
    studentService: StudentService,
    private val sessionStateManager: SessionStateManager
) : NanoHTTPD(8080) {

    private val gson = Gson()
    private val studentHandler = StudentRegistrationHandler(gson, sessionService, studentService)
    private val testPageHandler = TestPageHandler(sessionService, studentService)
    private val testSubmissionHandler = TestSubmissionHandler(studentService, sessionService)

    override fun serve(session: IHTTPSession?): Response {
        Log.d("LocalHttpServer", "Request: ${session?.method} ${session?.uri}")

        if (session == null) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Invalid request")
        }

        return when {
            session.method == Method.POST && session.uri.matches(Regex("/session/\\d+/student")) -> {
                studentHandler.handle(session)
            }

            session.method == Method.GET && session.uri.matches(Regex("/session/\\d+/test")) -> {
                testPageHandler.handle(session)
            }

            session.method == Method.POST && session.uri.matches(Regex("/session/\\d+/test/submit")) -> {
                testSubmissionHandler.handle(session)
            }

            session.method == Method.GET && session.uri == "/" -> {
                serveRegistrationPage(sessionStateManager)
            }

            else -> defaultHtml()
        }
    }

    private fun defaultHtml(): Response {
        val html = """
            <html>
            <head><title>Baum Server</title></head>
            <body>
                <h1>Сервер запущен</h1>
                <p>Для отправки данных используйте POST /session/{id}/student</p>
            </body>
            </html>
        """.trimIndent()
        return newFixedLengthResponse(Response.Status.OK, "text/html", html)
    }
}
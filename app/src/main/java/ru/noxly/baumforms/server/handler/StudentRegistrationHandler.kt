package ru.noxly.baumforms.server.handler

import com.google.gson.Gson
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ru.noxly.baumforms.db.entity.StudentEntity
import ru.noxly.baumforms.db.entity.TestSessionStatus
import ru.noxly.baumforms.service.StudentService
import ru.noxly.baumforms.service.TestSessionService

class StudentRegistrationHandler(
    private val gson: Gson,
    private val sessionService: TestSessionService,
    private val studentService: StudentService
) {
    fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response = runBlocking {
        val sessionId = session.uri.split("/")[2].toIntOrNull()
            ?: return@runBlocking NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.BAD_REQUEST, "text/plain", "Invalid session ID"
            )

        val postData = mutableMapOf<String, String>()
        try {
            session.parseBody(postData)
        } catch (e: Exception) {
            return@runBlocking NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.INTERNAL_ERROR, "text/plain", "Failed to parse form data"
            )
        }

        val params = session.parameters
        val fullName = params["fullName"]?.firstOrNull()?.trim()
        val group = params["group"]?.firstOrNull()?.trim()
        val mail = params["mail"]?.firstOrNull()?.trim()

        if (fullName.isNullOrBlank() || group.isNullOrBlank()) {
            return@runBlocking NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.BAD_REQUEST, "text/plain", "Full name and group are required"
            )
        }

        val testSession = sessionService.getSessionById(sessionId).first()
        if (testSession?.status != TestSessionStatus.STARTED) {
            return@runBlocking NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.FORBIDDEN, "text/plain", "Session not started"
            )
        }

        val student = StudentEntity(
            fullName = fullName,
            group = group,
            mail = mail,
            sessionId = sessionId
        )

        val studentId = studentService.addStudent(student)

        // ✅ Редирект на тест
        val redirectUrl = "/session/$sessionId/test?studentId=$studentId"
        return@runBlocking NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.REDIRECT,
            "text/plain",
            "Redirecting to test..."
        ).apply {
            addHeader("Location", redirectUrl)
        }
    }
}
package ru.noxly.baumforms.server.handler

import com.google.gson.Gson
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import ru.noxly.baumforms.model.StudentAnswer
import ru.noxly.baumforms.service.StudentService
import ru.noxly.baumforms.service.TestSessionService

class TestSubmissionHandler(
    private val studentService: StudentService,
    private val sessionService: TestSessionService,
    private val gson: Gson = Gson()
) {

    fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response = runBlocking {
        val uriParts = session.uri.split("/")
        val sessionId = uriParts.getOrNull(2)?.toIntOrNull()
        val studentId = session.parameters["studentId"]?.firstOrNull()?.toIntOrNull()

        if (sessionId == null || studentId == null) {
            return@runBlocking errorPage("Некорректный идентификатор сессии или студента.")
        }

        val student = studentService.getStudentById(studentId)
            ?: return@runBlocking errorPage(
                "Студент не найден.",
                NanoHTTPD.Response.Status.NOT_FOUND
            )

        if (!student.answersJson.isNullOrBlank()) {
            return@runBlocking errorPage(
                "Вы уже прошли этот тест.",
                NanoHTTPD.Response.Status.FORBIDDEN
            )
        }

        // Собираем ответы из формы
        session.parseBody(mutableMapOf())
        val formParams = session.parameters

        val answers = formParams.mapNotNull { (key, value) ->
            val match = Regex("""q(\d+)(\[\])?""").find(key) ?: return@mapNotNull null
            val index = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            val isMultiple = match.groupValues[2].isNotBlank()

            if (isMultiple) {
                StudentAnswer(
                    questionIndex = index,
                    selectedOptions = value.mapNotNull { it.toIntOrNull() }
                )
            } else {
                StudentAnswer(
                    questionIndex = index,
                    answerText = value.firstOrNull()
                )
            }
        }

        // Сохраняем
        val structuredJson = gson.toJson(answers)
        studentService.updateStudent(student.copy(answersJson = structuredJson))

        return@runBlocking successPage("Ваши ответы были успешно получены!")
    }

    private fun successPage(message: String): NanoHTTPD.Response {
        val html = """
        <html>
        <head><meta charset="utf-8"><title>Спасибо!</title></head>
        <body style="font-family: Arial, sans-serif; text-align: center; padding: 40px;">
            <div style="margin-bottom: 24px;">
                <h2 style="margin-bottom: 16px;">$message</h2>
                <br/>
                <p>Спасибо за участие в тестировании.</p>
            </div>
        </body>
        </html>
    """.trimIndent()

        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/html", html)
    }

    private fun errorPage(
        message: String,
        status: NanoHTTPD.Response.Status = NanoHTTPD.Response.Status.BAD_REQUEST
    ): NanoHTTPD.Response {
        val html = """
        <html>
        <head><meta charset="utf-8"><title>Ошибка</title></head>
        <body style="font-family: Arial, sans-serif; text-align: center; padding: 40px;">
            <div style="margin-bottom: 24px;">
                <h2 style="margin-bottom: 16px; color: #d32f2f;">$message</h2>
                <br/>
                <p>Повторная отправка ответов невозможна.</p>
            </div>
        </body>
        </html>
    """.trimIndent()

        return NanoHTTPD.newFixedLengthResponse(status, "text/html", html)
    }
}
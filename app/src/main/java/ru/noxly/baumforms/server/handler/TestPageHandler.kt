package ru.noxly.baumforms.server.handler

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ru.noxly.baumforms.model.AnswerType
import ru.noxly.baumforms.model.TestQuestion
import ru.noxly.baumforms.service.StudentService
import ru.noxly.baumforms.service.TestSessionService

class TestPageHandler(
    private val sessionService: TestSessionService,
    private val studentService: StudentService,
    private val gson: Gson = Gson()
) {
    fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response = runBlocking {
        val sessionId = session.uri.split("/")[2].toIntOrNull()
            ?: return@runBlocking NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.BAD_REQUEST, "text/plain", "Invalid session ID"
            )

        val studentId = session.parameters["studentId"]?.firstOrNull()?.toIntOrNull()
            ?: return@runBlocking NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.BAD_REQUEST, "text/plain", "Missing studentId"
            )

        val testSession = sessionService.getSessionById(sessionId).first()
            ?: return@runBlocking NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.NOT_FOUND, "text/plain", "Session not found"
            )

        val student = studentService.getStudentById(studentId)
            ?: return@runBlocking NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.NOT_FOUND, "text/plain", "Student not found"
            )

        val questionsJson = testSession.questionsJson
            ?: return@runBlocking NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.INTERNAL_ERROR,
                "text/plain",
                "No test content"
            )

        val questionType = object : TypeToken<List<TestQuestion>>() {}.type
        val questions = gson.fromJson<List<TestQuestion>>(questionsJson, questionType)

        val questionsHtml = questions.mapIndexed { index, q ->
            val qId = "q$index"
            val optionsHtml = when (q.type) {
                AnswerType.LECTURE, AnswerType.FREE_FORM -> """
                    <textarea name="$qId" rows="4" placeholder="Введите ваш ответ..."></textarea>
                """.trimIndent()

                AnswerType.SINGLE_CHOICE -> q.options.mapIndexed { optIndex, opt ->
                    """
                    <label class="option">
                        <input type="radio" name="$qId" value="${optIndex + 1}" />
                        ${opt}
                    </label>
                    """.trimIndent()
                }.joinToString(separator = "")

                AnswerType.MULTIPLE_CHOICE -> q.options.mapIndexed { optIndex, opt ->
                    """
                    <label class="option">
                        <input type="checkbox" name="${qId}[]" value="${optIndex + 1}" />
                        ${opt}
                    </label>
                    """.trimIndent()
                }.joinToString(separator = "")
            }

            """
            <div class="question-block">
                <p><strong>${index + 1}. ${q.question}</strong></p>
                $optionsHtml
            </div>
            """.trimIndent()
        }.joinToString("\n")

        val html = """
            <!DOCTYPE html>
            <html lang="ru">
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Тестирование</title>
                <style>
                    * {
                        box-sizing: border-box;
                    }

                    body {
                        font-family: "Segoe UI", Tahoma, sans-serif;
                        background-color: #f0f4f9;
                        margin: 0;
                        padding: 0;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                    }

                    .container {
                        background-color: white;
                        padding: 24px;
                        border-radius: 12px;
                        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
                        max-width: 800px;
                        width: 95%;
                        margin: 24px;
                    }

                    h2, p {
                        margin-bottom: 16px;
                        text-align: center;
                        color: #003ba3;
                    }

                    .question-block {
                        margin-bottom: 24px;
                        padding: 16px;
                        border: 1px solid #ccc;
                        border-radius: 8px;
                        background-color: #f9fafe;
                    }

                    .question-block p {
                        margin-bottom: 12px;
                        text-align: left;
                        color: #222;
                    }

                    textarea {
                        width: 100%;
                        padding: 10px;
                        font-size: 14px;
                        border: 1px solid #ccc;
                        border-radius: 6px;
                        resize: vertical;
                        margin-top: 6px;
                    }

                    .option {
                        display: block;
                        margin: 6px 0;
                        font-size: 14px;
                    }

                    button {
                        width: 100%;
                        padding: 12px;
                        background-color: #003ba3;
                        color: white;
                        font-size: 16px;
                        border: none;
                        border-radius: 6px;
                        cursor: pointer;
                        margin-top: 16px;
                    }

                    button:hover {
                        background-color: #1a49b2;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Тест: ${testSession.name}</h2>
                    <p>Студент: <strong>${student.fullName}</strong> (${student.group})</p>
                    
                    <form id="testForm" method="POST" action="/session/$sessionId/test/submit?studentId=$studentId">
                        $questionsHtml
                        <button type="submit">Отправить</button>
                    </form>
                </div>
                <script>
    const form = document.getElementById("testForm");
    form.addEventListener("submit", async function(event) {
        event.preventDefault();
        const formData = new FormData(form);
        const actionUrl = form.getAttribute("action");

        try {
            const response = await fetch(actionUrl, {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"
                },
                body: new URLSearchParams([...formData]).toString()
            });

            if (response.redirected) {
                window.location.href = response.url;
            } else {
                const html = await response.text();
                document.body.innerHTML = html;
            }
        } catch (err) {
            alert("❌ Сервер недоступен или выключен.");
        }
    });
</script>
            </body>
            </html>
        """.trimIndent()

        return@runBlocking NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.OK,
            "text/html",
            html
        )
    }
}
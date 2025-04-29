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

        val html = """
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Тестирование</title>
    <style>
        * { box-sizing: border-box; }
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
<div class="container" id="app">
    <h2>Загрузка...</h2>
</div>

<script>
const questions = ${gson.toJson(questions)};
const key = "shuffled_order_${sessionId}_${studentId}";
let shuffledQuestions = localStorage.getItem(key);

if (shuffledQuestions) {
    shuffledQuestions = JSON.parse(shuffledQuestions).map(i => questions[i]);
} else {
    const indices = questions.map((_, i) => i).sort(() => Math.random() - 0.5);
    localStorage.setItem(key, JSON.stringify(indices));
    shuffledQuestions = indices.map(i => questions[i]);
}

const answersKey = 'answers_${sessionId}_${studentId}';
const answers = JSON.parse(localStorage.getItem(answersKey) || '{}');

// 👉 определяем на каком вопросе продолжить
let current = Object.keys(answers).length;
if (current >= shuffledQuestions.length) current = shuffledQuestions.length - 1;

function saveAnswer(index, answer) {
    answers[index] = answer;
    localStorage.setItem(answersKey, JSON.stringify(answers));
}

function renderQuestion() {
    const container = document.getElementById("app");
    container.innerHTML = '';

    const q = shuffledQuestions[current];
    const questionDiv = document.createElement("div");
    questionDiv.className = "question-block";

    const title = document.createElement("p");
    title.innerHTML = "<strong>Вопрос " + (current + 1) + ":</strong> " + q.question;
    questionDiv.appendChild(title);

    const form = document.createElement("form");

    if (q.type === 'FREE_FORM' || q.type === 'LECTURE') {
        const textarea = document.createElement("textarea");
        textarea.name = "answer";
        textarea.placeholder = "Введите ваш ответ...";
        textarea.rows = 5;
        textarea.value = answers[current]?.answerText || '';
        form.appendChild(textarea);
    } else if (q.type === 'SINGLE_CHOICE') {
        q.options.forEach((opt, idx) => {
            const label = document.createElement("label");
            label.className = "option";
            const radio = document.createElement("input");
            radio.type = "radio";
            radio.name = "answer";
            radio.value = idx + 1;
            if (answers[current]?.selectedOptions?.includes(idx + 1)) radio.checked = true;
            label.appendChild(radio);
            label.appendChild(document.createTextNode(' ' + opt));
            form.appendChild(label);
        });
    } else if (q.type === 'MULTIPLE_CHOICE') {
        q.options.forEach((opt, idx) => {
            const label = document.createElement("label");
            label.className = "option";
            const checkbox = document.createElement("input");
            checkbox.type = "checkbox";
            checkbox.name = "answer";
            checkbox.value = idx + 1;
            if (answers[current]?.selectedOptions?.includes(idx + 1)) checkbox.checked = true;
            label.appendChild(checkbox);
            label.appendChild(document.createTextNode(' ' + opt));
            form.appendChild(label);
        });
    }

    container.appendChild(questionDiv);
    container.appendChild(form);

    const button = document.createElement("button");
    button.innerText = (current === shuffledQuestions.length - 1) ? "Отправить" : "Следующий вопрос";
    button.onclick = async (e) => {
        e.preventDefault();
        const formData = new FormData(form);
        const qIndex = questions.indexOf(q);

        if (q.type === 'FREE_FORM' || q.type === 'LECTURE') {
            saveAnswer(current, { questionIndex: qIndex, answerText: formData.get('answer') || '' });
        } else if (q.type === 'SINGLE_CHOICE') {
            const selected = formData.get('answer');
            saveAnswer(current, { questionIndex: qIndex, selectedOptions: selected ? [parseInt(selected)] : [] });
        } else if (q.type === 'MULTIPLE_CHOICE') {
            const selected = formData.getAll('answer').map(Number);
            saveAnswer(current, { questionIndex: qIndex, selectedOptions: selected });
        }

        if (current === shuffledQuestions.length - 1) {
            await submitAnswers();
        } else {
            current++;
            renderQuestion();
        }
    };

    container.appendChild(button);
}

async function submitAnswers() {
    const finalAnswers = Object.values(answers).sort((a, b) => a.questionIndex - b.questionIndex);
    try {
        const response = await fetch("${"/session/$sessionId/test/submit?studentId=$studentId"}", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
            body: new URLSearchParams(
                finalAnswers.flatMap(ans => {
                    if (ans.selectedOptions) {
                        return ans.selectedOptions.map(opt => ["q" + ans.questionIndex + "[]", opt]);
                    } else {
                        return [["q" + ans.questionIndex, ans.answerText]];
                    }
                })
            )
        });

        if (response.redirected) {
            // Удаляем сохранённые данные
            localStorage.removeItem(answersKey);
            localStorage.removeItem(key);
            window.location.href = response.url;
        } else {
            const html = await response.text();
            document.body.innerHTML = html;
        }
    } catch (err) {
        alert("❌ Не удалось отправить ответы. Проверьте соединение.");
    }
}

renderQuestion();
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
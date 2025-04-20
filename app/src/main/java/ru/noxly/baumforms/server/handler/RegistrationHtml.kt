package ru.noxly.baumforms.server.handler

import fi.iki.elonen.NanoHTTPD
import ru.noxly.baumforms.helper.SessionStateManager

fun serveRegistrationPage(sessionStateManager: SessionStateManager): NanoHTTPD.Response {
    val sessionId = sessionStateManager.getActiveSession()
        ?: return NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.NOT_FOUND,
            "text/plain",
            "Сессия не найдена"
        )

    val html = """
        <!DOCTYPE html>
        <html lang="ru">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Регистрация студента</title>
            <style>
                body {
                    font-family: "Segoe UI", Roboto, Arial, sans-serif;
                    background: #f1f4f8;
                    margin: 0;
                    padding: 0;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    min-height: 100vh;
                }

                .container {
                    background-color: #ffffff;
                    padding: 24px;
                    border-radius: 12px;
                    box-shadow: 0 8px 24px rgba(0, 48, 96, 0.1);
                    max-width: 420px;
                    width: 90%;
                    box-sizing: border-box;
                }

                h2 {
                    text-align: center;
                    margin-bottom: 24px;
                    color: #2B5DA9; /* Более светлый фирменный синий */
                    font-size: 24px;
                }

                label {
                    display: block;
                    margin-bottom: 16px;
                    color: #333;
                    font-weight: 500;
                }

                input[type="text"],
                input[type="email"] {
                    width: 100%;
                    padding: 12px;
                    border: 1px solid #ccc;
                    border-radius: 8px;
                    font-size: 15px;
                    box-sizing: border-box;
                    margin-top: 6px;
                    background: #f9f9f9;
                }

                input:focus {
                    outline: none;
                    border-color: #2B5DA9;
                    background-color: #fff;
                    box-shadow: 0 0 0 2px rgba(43, 93, 169, 0.1);
                }

                button {
                    width: 100%;
                    padding: 14px;
                    background-color: #2B5DA9;
                    color: white;
                    border: none;
                    border-radius: 8px;
                    font-size: 16px;
                    font-weight: bold;
                    cursor: pointer;
                    margin-top: 10px;
                    transition: background-color 0.3s ease;
                }

                button:hover {
                    background-color: #3C6FD1;
                }

                @media (max-width: 480px) {
                    h2 {
                        font-size: 20px;
                    }

                    .container {
                        padding: 20px;
                    }
                }
            </style>
        </head>
        <body>
            <div class="container">
                <h2>Регистрация студента</h2>
                <form method="POST" action="/session/$sessionId/student">
                    <label>ФИО:
                        <input type="text" name="fullName" required />
                    </label>
                    <label>Группа:
                        <input type="text" name="group" required />
                    </label>
                    <label>Email:
                        <input type="email" name="mail" />
                    </label>
                    <button type="submit">Начать тест</button>
                </form>
            </div>
        </body>
        </html>
    """.trimIndent()

    return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/html", html)
}



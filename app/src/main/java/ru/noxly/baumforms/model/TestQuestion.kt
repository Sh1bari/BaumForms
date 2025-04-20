package ru.noxly.baumforms.model

data class TestQuestion(
    val question: String,
    val type: AnswerType,
    val answer: String?,                    // оригинальное значение из Excel
    val options: List<String> = emptyList(),
    val correctAnswers: List<Int> = emptyList(),
    val maxScore: Int
)

data class ManualGrade(
    val questionIndex: Int,
    val answerText: String,
    val score: Int
)

data class StudentAnswer(
    val questionIndex: Int,               // индекс вопроса
    val answerText: String? = null,       // для текстовых типов
    val selectedOptions: List<Int>? = null // для одиночных/множественных ответов
)

enum class AnswerType {
    LECTURE,
    SINGLE_CHOICE,
    MULTIPLE_CHOICE,
    FREE_FORM;

    companion object {
        fun fromRaw(raw: String): AnswerType = when {
            raw.contains("множественный", ignoreCase = true) -> MULTIPLE_CHOICE
            raw.contains("единичный", ignoreCase = true) -> SINGLE_CHOICE
            raw.contains("свободной", ignoreCase = true) -> FREE_FORM
            else -> LECTURE
        }
    }
}
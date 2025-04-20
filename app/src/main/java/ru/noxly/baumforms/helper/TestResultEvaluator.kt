package ru.noxly.baumforms.helper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.noxly.baumforms.model.AnswerType
import ru.noxly.baumforms.model.StudentAnswer
import ru.noxly.baumforms.model.TestQuestion

object TestResultEvaluator {
    private val gson = Gson()

    fun evaluate(
        questionsJson: String,
        answersJson: String,
        manualScores: Map<Pair<Int, String>, Int> = emptyMap()
    ): Triple<Int, Int, Int> {
        val questions = gson.fromJson(questionsJson, object : TypeToken<List<TestQuestion>>() {}.type)
                as List<TestQuestion>
        val answers = gson.fromJson(answersJson, object : TypeToken<List<StudentAnswer>>() {}.type)
                as List<StudentAnswer>

        var totalScore = 0
        var pendingReview = 0
        var maxTotalScore = 0

        for ((index, question) in questions.withIndex()) {
            val answer = answers.find { it.questionIndex == index }
            maxTotalScore += question.maxScore

            when (question.type) {
                AnswerType.SINGLE_CHOICE, AnswerType.MULTIPLE_CHOICE -> {
                    val correctSet = question.correctAnswers.toSet()
                    val selectedSet = answer?.selectedOptions?.toSet() ?: emptySet()

                    val correctMatches = selectedSet.intersect(correctSet).size
                    val extraSelections = selectedSet.subtract(correctSet).size
                    val totalSelections = correctMatches + extraSelections

                    if (totalSelections > 0 && correctSet.isNotEmpty()) {
                        val ratio = correctMatches.toDouble() / totalSelections
                        val score = kotlin.math.ceil(ratio * question.maxScore).toInt()
                        totalScore += score
                    }
                }

                AnswerType.LECTURE, AnswerType.FREE_FORM -> {
                    val answerKey = index to (answer?.answerText?.trim().orEmpty())
                    if (manualScores.containsKey(answerKey)) {
                        totalScore += manualScores[answerKey] ?: 0
                    } else if (!answer?.answerText.isNullOrBlank()) {
                        pendingReview += 1
                    }
                }
            }
        }

        return Triple(totalScore, pendingReview, maxTotalScore)
    }

}
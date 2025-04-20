package ru.noxly.baumforms.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.noxly.baumforms.model.AnswerType
import ru.noxly.baumforms.model.TestQuestion

@Composable
fun ExcelQuestionPreview(question: TestQuestion) {
    val selectedSingle = remember { mutableStateOf(-1) }
    val selectedMultiple = remember { mutableStateListOf<Int>() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Вопрос:", style = MaterialTheme.typography.labelSmall)
            Text(question.question, style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(8.dp))

            Text("Тип: ${question.type}", style = MaterialTheme.typography.labelMedium)
            Text("Макс. балл: ${question.maxScore}", style = MaterialTheme.typography.labelMedium)

            Spacer(modifier = Modifier.height(8.dp))

            when (question.type) {
                AnswerType.LECTURE, AnswerType.FREE_FORM -> {
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Ответ") },
                        singleLine = false,
                        enabled = false
                    )
                }

                AnswerType.SINGLE_CHOICE -> {
                    question.options.forEachIndexed { index, option ->
                        val isCorrect = question.correctAnswers.contains(index)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = isCorrect,
                                onClick = {},
                                enabled = false
                            )
                            Text(
                                text = option,
                                modifier = Modifier.padding(start = 8.dp),
                                style = if (isCorrect) MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                                else MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                AnswerType.MULTIPLE_CHOICE -> {
                    question.options.forEachIndexed { index, option ->
                        val isChecked = question.correctAnswers.contains(index)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {},
                                enabled = false
                            )
                            Text(
                                text = option,
                                modifier = Modifier.padding(start = 8.dp),
                                style = if (isChecked) MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                                else MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            question.answer?.takeIf { it.isNotBlank() }?.let { rawAnswer ->
                Spacer(modifier = Modifier.height(6.dp))
                Text("Исходный ответ из Excel: $rawAnswer", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
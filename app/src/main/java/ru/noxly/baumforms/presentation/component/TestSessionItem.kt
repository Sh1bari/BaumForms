package ru.noxly.baumforms.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.noxly.baumforms.db.entity.TestSessionEntity
import ru.noxly.baumforms.db.entity.TestSessionStatus
import java.text.DateFormat
import java.util.Date

@Composable
fun TestSessionItem(
    session: TestSessionEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("#${session.id} ${session.name}", style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Статус: ${when (session.status) {
                    TestSessionStatus.CREATED -> "Не начат"
                    TestSessionStatus.STARTED -> "В процессе"
                    TestSessionStatus.FINISHED -> "Завершён"
                }}",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Создан: ${DateFormat.getDateTimeInstance().format(Date(session.createdAt))}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
package ru.noxly.baumforms.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ExcelFilePreview(
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    showDeleteIcon: Boolean = true
) {
    Box(
        modifier = modifier
            .size(80.dp)
            .clickable { onOpen() }
            .padding(4.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = "Файл Excel",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.Center)
        )

        if (showDeleteIcon) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Удалить",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .size(25.dp)
                    .align(Alignment.TopEnd)
                    .clickable { onDelete() }
                    .padding(4.dp)
            )
        }
    }
}
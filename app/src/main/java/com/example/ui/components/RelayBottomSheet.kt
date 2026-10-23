package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Post
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelayBottomSheet(
    post: Post,
    onDismiss: () -> Unit,
    onRelay: (quote: String?) -> Unit
) {
    var quote by remember { mutableStateOf("") }
    val maxChars = 280

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = QSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Relay this post",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = QOnSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Post preview
            Card(
                colors = CardDefaults.cardColors(containerColor = QSurfaceVariant),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text(
                    text = post.content,
                    maxLines = 3,
                    modifier = Modifier.padding(12.dp),
                    color = QOnSurfaceVariant
                )
            }

            OutlinedTextField(
                value = quote,
                onValueChange = { if (it.length <= maxChars) quote = it },
                label = { Text("Add a quote (optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Text(
                text = "${quote.length}/$maxChars",
                fontSize = 12.sp,
                color = QOnSurfaceVariant,
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp, bottom = 16.dp)
            )

            Button(
                onClick = { onRelay(quote.takeIf { it.isNotBlank() }) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = QRelayGreen)
            ) {
                Text("Relay")
            }
        }
    }
}

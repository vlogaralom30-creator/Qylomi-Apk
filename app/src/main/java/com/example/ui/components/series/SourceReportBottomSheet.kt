package com.example.ui.components.series

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceReportBottomSheet(
    sourceLabel: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmitReport: (reason: String, details: String?) -> Unit
) {
    var selectedReason by remember { mutableStateOf("broken") }
    var details by remember { mutableStateOf("") }

    val reasons = listOf(
        "broken" to "Playback broken / won't load",
        "buffering" to "Excessive buffering / slow stream",
        "wrong_audio" to "Wrong audio / missing dub",
        "wrong_subtitles" to "Missing or out-of-sync subtitles",
        "other" to "Other issue"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = QSurface,
        shape = SeriesTokens.shapeSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Report Stream Source",
                style = SeriesTypography.headline,
                color = QOnSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Report issue with $sourceLabel to help us fix broken streams.",
                style = SeriesTypography.body,
                color = QOnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Select Reason:",
                style = SeriesTypography.label,
                fontWeight = FontWeight.Bold,
                color = QOnSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                reasons.forEach { (reasonCode, reasonLabel) ->
                    val isSelected = selectedReason == reasonCode
                    Surface(
                        onClick = { selectedReason = reasonCode },
                        shape = SeriesTokens.shapeChip,
                        color = if (isSelected) QYellowLight else QSurfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) QYellowPrimary else QCardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedReason = reasonCode },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = QYellowDark,
                                    unselectedColor = QOutline
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = reasonLabel,
                                style = SeriesTypography.body,
                                color = if (isSelected) QYellowDark else QOnSurface,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                placeholder = { Text("Additional details (optional)...", style = SeriesTypography.body) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = QYellowPrimary,
                    unfocusedBorderColor = QOutline,
                    focusedContainerColor = QSurfaceVariant,
                    unfocusedContainerColor = QSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            YellowPillButton(
                text = "Submit Report",
                isLoading = isSubmitting,
                onClick = {
                    onSubmitReport(selectedReason, details.takeIf { it.isNotBlank() })
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

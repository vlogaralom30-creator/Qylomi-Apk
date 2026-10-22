package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.QCardBorder
import com.example.ui.theme.QYellowDark
import com.example.ui.theme.QYellowPrimary

data class TextPostTemplate(
    val id: String,
    val name: String,
    val backgroundBrush: Brush,
    val textColor: Color = Color.White,
    val accentColor: Color = QYellowPrimary
)

object TextPostTemplates {
    val templates = listOf(
        TextPostTemplate(
            id = "preset_default",
            name = "Midnight Obsidian",
            backgroundBrush = Brush.linearGradient(
                colors = listOf(Color(0xFF1E1E2C), Color(0xFF2A2A3D))
            ),
            textColor = Color(0xFFF1F1F5),
            accentColor = QYellowPrimary
        ),
        TextPostTemplate(
            id = "sunset_amber",
            name = "Sunset Amber",
            backgroundBrush = Brush.linearGradient(
                colors = listOf(Color(0xFF4A154B), Color(0xFF80154B), Color(0xFFE01E5A))
            ),
            textColor = Color.White,
            accentColor = Color(0xFFECB22E)
        ),
        TextPostTemplate(
            id = "neon_cyber",
            name = "Neon Cyber",
            backgroundBrush = Brush.linearGradient(
                colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
            ),
            textColor = Color(0xFFE0F7FA),
            accentColor = Color(0xFF00E5FF)
        ),
        TextPostTemplate(
            id = "warm_ember",
            name = "Warm Ember",
            backgroundBrush = Brush.linearGradient(
                colors = listOf(Color(0xFF2C001E), Color(0xFF5A102A), Color(0xFF8B263E))
            ),
            textColor = Color(0xFFFFF0F5),
            accentColor = Color(0xFFFFB74D)
        ),
        TextPostTemplate(
            id = "deep_emerald",
            name = "Deep Emerald",
            backgroundBrush = Brush.linearGradient(
                colors = listOf(Color(0xFF002B2B), Color(0xFF004D40), Color(0xFF00695C))
            ),
            textColor = Color(0xFFE0F2F1),
            accentColor = Color(0xFF64FFDA)
        ),
        TextPostTemplate(
            id = "electric_yellow",
            name = "Q Signature Yellow",
            backgroundBrush = Brush.linearGradient(
                colors = listOf(Color(0xFF2A2200), Color(0xFF4A3D00), Color(0xFF1E1900))
            ),
            textColor = Color(0xFFFFFDE7),
            accentColor = QYellowPrimary
        )
    )

    fun getTemplate(id: String?): TextPostTemplate {
        return templates.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: templates.first()
    }
}

@Composable
fun StyledTextPostCard(
    content: String,
    templateId: String? = "preset_default",
    category: String? = null,
    modifier: Modifier = Modifier
) {
    val template = remember(templateId) { TextPostTemplates.getTemplate(templateId) }
    var isExpanded by remember { mutableStateOf(false) }
    val isLongText = content.length > 220

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(1.dp, QCardBorder.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("styled_text_post_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(template.backgroundBrush)
                .padding(20.dp)
                .animateContentSize()
        ) {
            Column {
                if (!category.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(template.accentColor.copy(alpha = 0.25f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = category.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = template.accentColor
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Giant accent quotation mark
                Text(
                    text = "“",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = template.accentColor.copy(alpha = 0.6f),
                    lineHeight = 20.sp
                )

                Text(
                    text = content,
                    color = template.textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp,
                    maxLines = if (isExpanded || !isLongText) Int.MAX_VALUE else 5,
                    overflow = TextOverflow.Ellipsis
                )

                if (isLongText) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isExpanded) "Show Less" else "Read Full Post...",
                        color = template.accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable { isExpanded = !isExpanded }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

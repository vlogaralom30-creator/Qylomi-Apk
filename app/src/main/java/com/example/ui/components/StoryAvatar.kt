package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.UserProfile
import com.example.ui.theme.QOnSurface
import com.example.ui.theme.QOnYellow
import com.example.ui.theme.QYellowDark
import com.example.ui.theme.QYellowPrimary

@Composable
fun StoryAvatar(
    profile: UserProfile,
    isCurrentUser: Boolean = false,
    onClick: () -> Unit
) {
    val ringBrush = Brush.sweepGradient(
        listOf(
            QYellowPrimary,
            QYellowDark,
            Color(0xFFFFE082),
            QYellowPrimary
        )
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(68.dp)
                .border(2.5.dp, ringBrush, CircleShape)
                .padding(3.dp)
        ) {
            UserAvatar(
                avatarUrl = profile.avatarUrl,
                initials = profile.initials,
                size = 58.dp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isCurrentUser) "Your Story" else profile.effectiveDisplayName.split(" ").first(),
            fontSize = 11.sp,
            fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium,
            color = QOnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.size(width = 68.dp, height = 16.dp)
        )
    }
}

@Composable
fun UserAvatar(
    avatarUrl: String?,
    initials: String,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    if (!avatarUrl.isNullOrBlank()) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = "Avatar",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFFE2E8F0))
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(QYellowPrimary)
        ) {
            Text(
                text = initials.take(2),
                color = QOnYellow,
                fontSize = (size.value * 0.4f).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

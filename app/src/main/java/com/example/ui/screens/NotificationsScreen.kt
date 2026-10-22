package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NotificationItem
import com.example.data.model.UserProfile
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatTimeAgo
import com.example.ui.theme.QBackground
import com.example.ui.theme.QCardBorder
import com.example.ui.theme.QLikeRed
import com.example.ui.theme.QOnSurface
import com.example.ui.theme.QOnSurfaceVariant
import com.example.ui.theme.QRelayGreen
import com.example.ui.theme.QSurface
import com.example.ui.theme.QYellowDark
import com.example.ui.theme.QYellowLight

@Composable
fun NotificationsScreen(
    notifications: List<NotificationItem>,
    onRefresh: () -> Unit
) {
    val displayNotifications = if (notifications.isNotEmpty()) {
        notifications
    } else {
        listOf(
            NotificationItem(
                id = "1",
                recipientId = "curr",
                type = "appreciate",
                actorProfile = UserProfile(displayName = "Rony Ahammad", username = "ronyvi"),
                createdAt = "2026-10-22T10:00:00Z"
            ),
            NotificationItem(
                id = "2",
                recipientId = "curr",
                type = "track",
                actorProfile = UserProfile(displayName = "ChatGpt", username = "chatgpt"),
                createdAt = "2026-10-22T08:30:00Z"
            ),
            NotificationItem(
                id = "3",
                recipientId = "curr",
                type = "reply",
                actorProfile = UserProfile(displayName = "Rofi", username = "rafi"),
                createdAt = "2026-10-21T16:00:00Z"
            ),
            NotificationItem(
                id = "4",
                recipientId = "curr",
                type = "relay",
                actorProfile = UserProfile(displayName = "Qylomi Creator", username = "creator"),
                createdAt = "2026-10-21T12:00:00Z"
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(QSurface)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Notifications & Activity",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = QOnSurface
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp, top = 8.dp)
        ) {
            items(displayNotifications, key = { it.id }) { item ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = QSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, QCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        val (icon, iconBg, iconColor) = when (item.type) {
                            "appreciate" -> Triple(Icons.Default.Favorite, QLikeRed.copy(alpha = 0.15f), QLikeRed)
                            "track" -> Triple(Icons.Default.PersonAdd, QRelayGreen.copy(alpha = 0.15f), QRelayGreen)
                            "reply" -> Triple(Icons.Default.ChatBubble, QYellowLight, QYellowDark)
                            "relay" -> Triple(Icons.Default.Repeat, QRelayGreen.copy(alpha = 0.15f), QRelayGreen)
                            else -> Triple(Icons.Default.AutoAwesome, QYellowLight, QYellowDark)
                        }

                        if (item.actorProfile != null) {
                            UserAvatar(
                                avatarUrl = item.actorProfile.avatarUrl,
                                initials = item.actorProfile.initials,
                                size = 40.dp
                            )
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(iconBg)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = iconColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.displayMessage,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = QOnSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatTimeAgo(item.createdAt),
                                fontSize = 11.sp,
                                color = QOnSurfaceVariant
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(iconBg)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

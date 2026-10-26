package com.example.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.theme.QOnSurface
import com.example.ui.theme.QVerifiedBlue

@Composable
fun UserNameWithBadge(
    user: UserProfile,
    modifier: Modifier = Modifier,
    showBadge: Boolean = true
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Text(
            text = user.effectiveDisplayName,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = QOnSurface
        )
        if (showBadge && user.isProfileComplete) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Verified",
                tint = QVerifiedBlue,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

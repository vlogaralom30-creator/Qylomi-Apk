package com.example.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.UserProfile
import com.example.util.ProfileUtils
import com.example.ui.theme.QVerifiedBlue

@Composable
fun ProfileBadge(
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    if (ProfileUtils.isProfileComplete(profile)) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Complete profile",
            tint = QVerifiedBlue,
            modifier = modifier.size(14.dp)
        )
    }
}

package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProfileUpdateRequest
import com.example.data.model.UserProfile
import com.example.ui.components.UserAvatar
import com.example.ui.theme.QBackground
import com.example.ui.theme.QCardBorder
import com.example.ui.theme.QLikeRed
import com.example.ui.theme.QOnSurface
import com.example.ui.theme.QOnSurfaceVariant
import com.example.ui.theme.QOnYellow
import com.example.ui.theme.QSurface
import com.example.ui.theme.QSurfaceVariant
import com.example.ui.theme.QYellowDark
import com.example.ui.theme.QYellowLight
import com.example.ui.theme.QYellowPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditProfileScreen(
    currentProfile: UserProfile,
    onBack: () -> Unit,
    onSave: (userId: String, updates: ProfileUpdateRequest, onDone: (Boolean, String?) -> Unit) -> Unit
) {
    BackHandler {
        onBack()
    }

    var displayName by remember { mutableStateOf(currentProfile.displayName ?: "") }
    var username by remember { mutableStateOf(currentProfile.username ?: "") }
    var headline by remember { mutableStateOf(currentProfile.headline ?: "") }
    var bio by remember { mutableStateOf(currentProfile.bio ?: "") }
    var website by remember { mutableStateOf(currentProfile.website ?: "") }
    var avatarUrl by remember { mutableStateOf(currentProfile.avatarUrl ?: "") }
    var coverUrl by remember { mutableStateOf(currentProfile.coverUrl ?: "") }
    var location by remember { mutableStateOf(currentProfile.location ?: "") }
    var work by remember { mutableStateOf(currentProfile.work ?: "") }
    var education by remember { mutableStateOf(currentProfile.education ?: "") }
    var ageString by remember { mutableStateOf(currentProfile.age?.toString() ?: "") }
    var gender by remember { mutableStateOf(currentProfile.gender ?: "Male") }

    // Skills
    var skillsList by remember { mutableStateOf(currentProfile.skills ?: emptyList()) }
    var newSkillInput by remember { mutableStateOf("") }

    // Social Links
    val existingLinks = currentProfile.socialLinks ?: emptyMap()
    var linkX by remember { mutableStateOf(existingLinks["x"] ?: "") }
    var linkGithub by remember { mutableStateOf(existingLinks["github"] ?: "") }
    var linkInstagram by remember { mutableStateOf(existingLinks["instagram"] ?: "") }
    var linkLinkedin by remember { mutableStateOf(existingLinks["linkedin"] ?: "") }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val genderOptions = listOf("Male", "Female", "Non-Binary", "Custom")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(QSurface)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = QOnSurface)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Edit Profile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = QOnSurface
                )
            }

            Button(
                onClick = {
                    isSaving = true
                    errorMessage = null

                    val updatedSocials = mutableMapOf<String, String>().apply {
                        if (linkX.isNotBlank()) put("x", linkX.trim())
                        if (linkGithub.isNotBlank()) put("github", linkGithub.trim())
                        if (linkInstagram.isNotBlank()) put("instagram", linkInstagram.trim())
                        if (linkLinkedin.isNotBlank()) put("linkedin", linkLinkedin.trim())
                    }

                    val request = ProfileUpdateRequest(
                        displayName = displayName.trim(),
                        username = username.trim(),
                        headline = headline.trim(),
                        bio = bio.trim(),
                        website = website.trim(),
                        avatarUrl = avatarUrl.trim().takeIf { it.isNotBlank() },
                        coverUrl = coverUrl.trim().takeIf { it.isNotBlank() },
                        location = location.trim(),
                        work = work.trim(),
                        education = education.trim(),
                        age = ageString.toIntOrNull(),
                        gender = gender,
                        skills = skillsList,
                        socialLinks = updatedSocials
                    )

                    onSave(currentProfile.id, request) { success, err ->
                        isSaving = false
                        if (success) {
                            onBack()
                        } else {
                            errorMessage = err ?: "Failed to save profile"
                        }
                    }
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = QYellowPrimary,
                    contentColor = QOnYellow
                ),
                modifier = Modifier.testTag("save_profile_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = QOnYellow,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Avatar Preview & URL
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = QSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, QCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(3.dp, QYellowPrimary, CircleShape)
                    ) {
                        UserAvatar(
                            avatarUrl = avatarUrl.takeIf { it.isNotBlank() },
                            initials = displayName.take(2).ifEmpty { "Q" },
                            size = 70.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = avatarUrl,
                        onValueChange = { avatarUrl = it },
                        label = { Text("Avatar Image URL") },
                        placeholder = { Text("https://example.com/photo.jpg") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_avatar_url"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = QYellowPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = coverUrl,
                        onValueChange = { coverUrl = it },
                        label = { Text("Cover Banner Image URL") },
                        placeholder = { Text("https://example.com/banner.jpg") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_cover_url"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = QYellowPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Identity Fields
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = QSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, QCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Basic Info", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = QOnSurface)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_input_display_name"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = QYellowPrimary)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_input_username"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = QYellowPrimary)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = headline,
                        onValueChange = { headline = it },
                        label = { Text("Headline / Catchphrase") },
                        placeholder = { Text("e.g. Creative Content Creator") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = QYellowPrimary)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Bio") },
                        placeholder = { Text("Tell the Q community about yourself...") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = QYellowPrimary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Additional details: Location, Work, Education, Website, Age, Gender
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = QSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, QCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Details & Demographics", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = QOnSurface)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        placeholder = { Text("City, Country") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = QYellowPrimary)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = work,
                        onValueChange = { work = it },
                        label = { Text("Work / Role") },
                        placeholder = { Text("e.g. Software Engineer, Designer") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = QYellowPrimary)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = education,
                        onValueChange = { education = it },
                        label = { Text("Education") },
                        placeholder = { Text("e.g. University, Self-Taught") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = QYellowPrimary)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = website,
                        onValueChange = { website = it },
                        label = { Text("Website") },
                        placeholder = { Text("https://yourwebsite.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = QYellowPrimary)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = ageString,
                            onValueChange = { ageString = it.filter { char -> char.isDigit() } },
                            label = { Text("Age") },
                            placeholder = { Text("25") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = QYellowPrimary)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Gender", fontSize = 12.sp, color = QOnSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(QSurfaceVariant)
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                genderOptions.take(2).forEach { g ->
                                    val isSelected = gender == g
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) QYellowPrimary else Color.Transparent)
                                            .clickable { gender = g }
                                            .padding(horizontal = 8.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = g,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) QOnYellow else QOnSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Skills & Expertise
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = QSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, QCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Skills & Expertise", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = QOnSurface)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newSkillInput,
                            onValueChange = { newSkillInput = it },
                            placeholder = { Text("Add skill (e.g. Kotlin)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = QYellowPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newSkillInput.isNotBlank() && !skillsList.contains(newSkillInput.trim())) {
                                    skillsList = skillsList + newSkillInput.trim()
                                    newSkillInput = ""
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(QYellowPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Skill", tint = QOnYellow)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        skillsList.forEach { skill ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(QYellowLight)
                                    .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                            ) {
                                Text(
                                    text = skill,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = QYellowDark
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = QYellowDark,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            skillsList = skillsList.filter { it != skill }
                                        }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Social Links Fields
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = QSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, QCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Social Profiles", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = QOnSurface)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = linkX,
                        onValueChange = { linkX = it },
                        label = { Text("X (Twitter) URL") },
                        placeholder = { Text("https://x.com/username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = QYellowPrimary)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = linkGithub,
                        onValueChange = { linkGithub = it },
                        label = { Text("GitHub URL") },
                        placeholder = { Text("https://github.com/username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = QYellowPrimary)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = linkInstagram,
                        onValueChange = { linkInstagram = it },
                        label = { Text("Instagram URL") },
                        placeholder = { Text("https://instagram.com/username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = QYellowPrimary)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = linkLinkedin,
                        onValueChange = { linkLinkedin = it },
                        label = { Text("LinkedIn URL") },
                        placeholder = { Text("https://linkedin.com/in/username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = QYellowPrimary)
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage ?: "",
                    color = QLikeRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Big Save Button
            Button(
                onClick = {
                    isSaving = true
                    errorMessage = null

                    val updatedSocials = mutableMapOf<String, String>().apply {
                        if (linkX.isNotBlank()) put("x", linkX.trim())
                        if (linkGithub.isNotBlank()) put("github", linkGithub.trim())
                        if (linkInstagram.isNotBlank()) put("instagram", linkInstagram.trim())
                        if (linkLinkedin.isNotBlank()) put("linkedin", linkLinkedin.trim())
                    }

                    val request = ProfileUpdateRequest(
                        displayName = displayName.trim(),
                        username = username.trim(),
                        headline = headline.trim(),
                        bio = bio.trim(),
                        website = website.trim(),
                        avatarUrl = avatarUrl.trim().takeIf { it.isNotBlank() },
                        coverUrl = coverUrl.trim().takeIf { it.isNotBlank() },
                        location = location.trim(),
                        work = work.trim(),
                        education = education.trim(),
                        age = ageString.toIntOrNull(),
                        gender = gender,
                        skills = skillsList,
                        socialLinks = updatedSocials
                    )

                    onSave(currentProfile.id, request) { success, err ->
                        isSaving = false
                        if (success) {
                            onBack()
                        } else {
                            errorMessage = err ?: "Failed to save profile"
                        }
                    }
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = QYellowPrimary,
                    contentColor = QOnYellow
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = QOnYellow,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save Changes to Supabase", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

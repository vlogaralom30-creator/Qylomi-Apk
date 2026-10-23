package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GroupCommunity
import com.example.ui.theme.QCardBorder
import com.example.ui.theme.QOnSurface
import com.example.ui.theme.QOnSurfaceVariant
import com.example.ui.theme.QOnYellow
import com.example.ui.theme.QSurface
import com.example.ui.theme.QYellowDark
import com.example.ui.theme.QYellowLight
import com.example.ui.theme.QYellowPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupCommunityBottomSheet(
    groups: List<GroupCommunity>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onToggleJoinGroup: (GroupCommunity) -> Unit,
    onCreateGroup: (name: String, description: String, category: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCreateForm by remember { mutableStateOf(false) }

    var groupName by remember { mutableStateOf("") }
    var groupDesc by remember { mutableStateOf("") }
    var groupCategory by remember { mutableStateOf("Tech & Dev") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = QSurface,
        modifier = Modifier.testTag("group_community_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = "Communities",
                        tint = QYellowDark,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (showCreateForm) "Create Community" else "Groups & Communities",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = QOnSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!showCreateForm) {
                        IconButton(onClick = { showCreateForm = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Create Group", tint = QYellowDark)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = QOnSurface)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (showCreateForm) {
                // Create Group Form
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name", color = QOnSurfaceVariant) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = QYellowPrimary,
                            unfocusedBorderColor = QOnSurfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = groupDesc,
                        onValueChange = { groupDesc = it },
                        label = { Text("Description & Mission", color = QOnSurfaceVariant) },
                        minLines = 3,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = QYellowPrimary,
                            unfocusedBorderColor = QOnSurfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showCreateForm = false },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Back")
                        }

                        Button(
                            onClick = {
                                if (groupName.isNotBlank()) {
                                    onCreateGroup(groupName.trim(), groupDesc.trim(), groupCategory)
                                    showCreateForm = false
                                }
                            },
                            enabled = groupName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = QYellowPrimary,
                                contentColor = QOnYellow
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Create Group", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Group List
                if (groups.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = QOnSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No communities created yet.",
                                color = QOnSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(groups, key = { it.id }) { group ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = QSurface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, QCardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(QYellowLight)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Group,
                                            contentDescription = null,
                                            tint = QYellowDark,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = group.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = QOnSurface
                                        )
                                        if (!group.description.isNullOrBlank()) {
                                            Text(
                                                text = group.description,
                                                fontSize = 12.sp,
                                                color = QOnSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                        Text(
                                            text = "${group.membersCount} Members • ${group.category ?: "general"}",
                                            fontSize = 11.sp,
                                            color = QYellowDark,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    if (group.isJoinedByMe) {
                                        OutlinedButton(
                                            onClick = { onToggleJoinGroup(group) },
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Joined", fontSize = 12.sp)
                                        }
                                    } else {
                                        Button(
                                            onClick = { onToggleJoinGroup(group) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = QYellowPrimary,
                                                contentColor = QOnYellow
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Text("Join", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

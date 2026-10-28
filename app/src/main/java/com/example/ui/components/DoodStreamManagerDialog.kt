package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.DoodStreamClient
import com.example.data.model.DoodAccountResult
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoodStreamManagerDialog(
    initialApiKey: String = "580872kmr7gfme6zjespee",
    onSelectEmbedUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var apiKey by remember { mutableStateOf(initialApiKey) }
    var accountResult by remember { mutableStateOf<DoodAccountResult?>(null) }
    var isLoadingInfo by remember { mutableStateOf(false) }

    // Remote upload state
    var uploadUrl by remember { mutableStateOf("") }
    var uploadTitle by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var uploadStatusMsg by remember { mutableStateOf<String?>(null) }

    // File list state
    var filesList by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var isLoadingFiles by remember { mutableStateOf(false) }
    var searchTerm by remember { mutableStateOf("") }

    fun fetchAccountInfo() {
        if (apiKey.isBlank()) return
        scope.launch {
            isLoadingInfo = true
            try {
                val response = withContext(Dispatchers.IO) {
                    DoodStreamClient.api.getAccountInfo(apiKey.trim())
                }
                if (response.isSuccessful) {
                    accountResult = response.body()?.result
                } else {
                    Toast.makeText(context, "Failed to load DoodStream account", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoadingInfo = false
            }
        }
    }

    fun fetchFiles() {
        if (apiKey.isBlank()) return
        scope.launch {
            isLoadingFiles = true
            try {
                val response = withContext(Dispatchers.IO) {
                    if (searchTerm.isNotBlank()) {
                        DoodStreamClient.api.searchVideos(apiKey.trim(), searchTerm.trim())
                    } else {
                        DoodStreamClient.api.getFileList(apiKey.trim())
                    }
                }
                if (response.isSuccessful) {
                    val map = response.body()
                    val resObj = map?.get("result")
                    @Suppress("UNCHECKED_CAST")
                    val list = when (resObj) {
                        is List<*> -> resObj as? List<Map<String, Any?>>
                        is Map<*, *> -> {
                            (resObj["files"] as? List<*>) as? List<Map<String, Any?>>
                        }
                        else -> null
                    }
                    filesList = list ?: emptyList()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "File list error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoadingFiles = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchAccountInfo()
        fetchFiles()
    }

    fun startRemoteUpload() {
        if (uploadUrl.isBlank()) {
            Toast.makeText(context, "Enter a direct video URL to upload", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            isUploading = true
            uploadStatusMsg = "Starting remote upload..."
            try {
                val response = withContext(Dispatchers.IO) {
                    DoodStreamClient.api.remoteUpload(
                        apiKey = apiKey.trim(),
                        url = uploadUrl.trim(),
                        newTitle = uploadTitle.trim().takeIf { it.isNotBlank() }
                    )
                }
                if (response.isSuccessful) {
                    val body = response.body()
                    val msg = body?.get("msg") as? String
                    val fileCode = (body?.get("result") as? Map<*, *>)?.get("filecode") as? String
                        ?: (body?.get("result") as? String)
                    uploadStatusMsg = "Success! File code: $fileCode ($msg)"
                    Toast.makeText(context, "Remote upload queued!", Toast.LENGTH_SHORT).show()
                    uploadUrl = ""
                    uploadTitle = ""
                    fetchFiles()
                } else {
                    uploadStatusMsg = "Upload failed: ${response.code()}"
                }
            } catch (e: Exception) {
                uploadStatusMsg = "Error: ${e.localizedMessage}"
            } finally {
                isUploading = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = QSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = QYellowPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("DoodStream Manager & Uploader", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // API Key input & Account Stats
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("DoodStream API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = QYellowPrimary,
                        unfocusedBorderColor = QCardBorder,
                        focusedLabelColor = QYellowPrimary,
                        cursorColor = QYellowPrimary
                    ),
                    trailingIcon = {
                        IconButton(onClick = {
                            fetchAccountInfo()
                            fetchFiles()
                        }) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = QYellowPrimary)
                        }
                    }
                )

                if (accountResult != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = QSurfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Email: ${accountResult?.email ?: "N/A"} | Balance: \$${accountResult?.balance ?: "0.00"}", color = Color.White, fontSize = 12.sp)
                            val storageMb = (accountResult?.storageLeft ?: 0L) / (1024 * 1024)
                            Text("Storage Left: $storageMb MB", color = QOnSurfaceVariant, fontSize = 11.sp)
                        }
                    }
                }

                // Remote Upload Section
                Card(
                    colors = CardDefaults.cardColors(containerColor = QSurfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Remote Upload via URL", fontWeight = FontWeight.Bold, color = QYellowPrimary, fontSize = 13.sp)
                        OutlinedTextField(
                            value = uploadUrl,
                            onValueChange = { uploadUrl = it },
                            placeholder = { Text("https://example.com/video.mp4") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uploadTitle,
                            onValueChange = { uploadTitle = it },
                            placeholder = { Text("Optional Video Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uploadStatusMsg != null) {
                                Text(uploadStatusMsg!!, color = QYellowLight, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            }
                            Button(
                                onClick = ::startRemoteUpload,
                                enabled = !isUploading,
                                colors = ButtonDefaults.buttonColors(containerColor = QYellowPrimary, contentColor = Color.Black)
                            ) {
                                if (isUploading) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text("Upload URL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Search & File List
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchTerm,
                        onValueChange = { searchTerm = it },
                        placeholder = { Text("Search files...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = ::fetchFiles) {
                        Text("Search", fontSize = 12.sp)
                    }
                }

                Text("Your DoodStream Files (Tap to Use Embed URL)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)

                if (isLoadingFiles) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = QYellowPrimary)
                    }
                } else if (filesList.isEmpty()) {
                    Text("No files found or unable to list files.", color = QOnSurfaceVariant, fontSize = 12.sp)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filesList) { file ->
                            val code = file["file_code"] as? String ?: file["filecode"] as? String ?: ""
                            val title = file["title"] as? String ?: "Untitled Video"
                            val embedUrl = "https://dood.to/e/$code"

                            Card(
                                colors = CardDefaults.cardColors(containerColor = QSurfaceVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectEmbedUrl(embedUrl)
                                        onDismiss()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                        Text(text = "Code: $code", color = QYellowLight, fontSize = 11.sp)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("DoodEmbed", embedUrl))
                                                Toast.makeText(context, "Copied embed link!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                        Button(
                                            onClick = {
                                                onSelectEmbedUrl(embedUrl)
                                                onDismiss()
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Use", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = QYellowPrimary)
            }
        }
    )
}

package com.example.ui.screens.studio

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AdminStudioViewModel
import com.example.ui.viewmodel.AdminTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStudioScreen(
    viewModel: AdminStudioViewModel,
    currentUser: UserProfile?,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Security & Role Guard: Admin only!
    LaunchedEffect(currentUser) {
        if (currentUser == null || !currentUser.isAdmin) {
            onBack()
        }
    }

    val selectedTab by viewModel.selectedTab.collectAsState()
    val allSeries by viewModel.allSeries.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val allShelves by viewModel.allShelves.collectAsState()
    val sourceReports by viewModel.sourceReports.collectAsState()
    val reportFilter by viewModel.reportFilter.collectAsState()
    val searchedUsers by viewModel.searchedUsers.collectAsState()
    val bannerMessage by viewModel.bannerMessage.collectAsState()

    // Dialog state holders
    var seriesToBlock by remember { mutableStateOf<Series?>(null) }
    var seriesToDelete by remember { mutableStateOf<Series?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<ContentCategory?>(null) }
    var showShelfDialog by remember { mutableStateOf(false) }
    var shelfToEdit by remember { mutableStateOf<Shelf?>(null) }
    var userToChangeRole by remember { mutableStateOf<UserProfile?>(null) }

    LaunchedEffect(bannerMessage) {
        bannerMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.setBanner(null)
        }
    }

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Admin Studio", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Platform Oversight & Catalog Management", fontSize = 11.sp, color = QYellowDark)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = QBackground,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = QBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = QSurface,
                contentColor = QYellowDark
            ) {
                AdminTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Text(
                                text = when (tab) {
                                    AdminTab.MODERATION -> "Moderation"
                                    AdminTab.SHELVES -> "Shelves"
                                    AdminTab.CATEGORIES -> "Categories"
                                    AdminTab.ROLES -> "Roles"
                                },
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    AdminTab.MODERATION -> ModerationTabView(
                        allSeries = allSeries,
                        reports = sourceReports,
                        currentFilter = reportFilter,
                        onFilterChange = { viewModel.setReportFilter(it) },
                        onBlockClick = { seriesToBlock = it },
                        onUnblockClick = { s ->
                            viewModel.unblockSeries(s.id) { success, msg ->
                                Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onTogglePublish = { s ->
                            viewModel.togglePublishAdmin(s) { success, msg ->
                                Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDeleteSeries = { seriesToDelete = it },
                        onMarkReportHandled = { rId ->
                            viewModel.markReportHandled(rId) { success, msg ->
                                Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onReopenReport = { rId ->
                            viewModel.reopenReport(rId) { success, msg ->
                                Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDisableSource = { srcId ->
                            viewModel.disableReportedSource(srcId) { success, msg ->
                                Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDeleteReport = { rId ->
                            viewModel.deleteReport(rId) { success, msg ->
                                Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    AdminTab.SHELVES -> ShelvesTabView(
                        shelves = allShelves,
                        categories = allCategories,
                        onAddShelf = {
                            shelfToEdit = null
                            showShelfDialog = true
                        },
                        onEditShelf = { s ->
                            shelfToEdit = s
                            showShelfDialog = true
                        },
                        onDeleteShelf = { sId ->
                            viewModel.deleteShelf(sId) { success, msg ->
                                Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    AdminTab.CATEGORIES -> CategoriesTabView(
                        categories = allCategories,
                        onAddCategory = {
                            categoryToEdit = null
                            showCategoryDialog = true
                        },
                        onEditCategory = { c ->
                            categoryToEdit = c
                            showCategoryDialog = true
                        },
                        onDeleteCategory = { cId ->
                            viewModel.deleteCategory(cId) { success, msg ->
                                Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    AdminTab.ROLES -> RolesTabView(
                        users = searchedUsers,
                        onSearch = { q -> viewModel.searchUsers(q) },
                        onChangeRoleClick = { u -> userToChangeRole = u }
                    )
                }
            }
        }
    }

    // Dialog: Block Series with Reason
    seriesToBlock?.let { series ->
        var blockReason by remember { mutableStateOf("Violation of content guidelines") }

        AlertDialog(
            onDismissRequest = { seriesToBlock = null },
            title = { Text("Block '${series.title}'") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Blocking immediately unpublishes this series and prevents creator modification.", fontSize = 13.sp)
                    OutlinedTextField(
                        value = blockReason,
                        onValueChange = { blockReason = it },
                        label = { Text("Takedown / Block Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sId = series.id
                        val r = blockReason.trim().ifBlank { "Takedown by administrator" }
                        seriesToBlock = null
                        viewModel.blockSeries(sId, r) { success, msg ->
                            Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White)
                ) {
                    Text("Block Series")
                }
            },
            dismissButton = {
                TextButton(onClick = { seriesToBlock = null }) { Text("Cancel") }
            },
            containerColor = QSurface,
            titleContentColor = Color.White,
            textContentColor = QOnSurfaceVariant
        )
    }

    // Dialog: Delete Series Confirm
    seriesToDelete?.let { series ->
        AlertDialog(
            onDismissRequest = { seriesToDelete = null },
            title = { Text("Delete '${series.title}' permanently?") },
            text = { Text("This will cascade delete all episodes and streaming sources.") },
            confirmButton = {
                Button(
                    onClick = {
                        val sId = series.id
                        seriesToDelete = null
                        viewModel.deleteSeriesAdmin(sId) { success, msg ->
                            Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { seriesToDelete = null }) { Text("Cancel") }
            },
            containerColor = QSurface,
            titleContentColor = Color.White,
            textContentColor = QOnSurfaceVariant
        )
    }

    // Dialog: Add / Edit Category
    if (showCategoryDialog) {
        CategoryEditorDialog(
            existing = categoryToEdit,
            onDismiss = { showCategoryDialog = false },
            onSave = { slug, name, sortOrder, isActive ->
                showCategoryDialog = false
                viewModel.saveCategory(categoryToEdit?.id, slug, name, sortOrder, isActive) { success, msg ->
                    Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Dialog: Add / Edit Shelf
    if (showShelfDialog) {
        ShelfEditorDialog(
            existing = shelfToEdit,
            categories = allCategories,
            onDismiss = { showShelfDialog = false },
            onSave = { slug, title, type, layout, config, catId, pos, isActive, maxItems ->
                showShelfDialog = false
                viewModel.saveShelf(shelfToEdit?.id, slug, title, type, layout, config, catId, pos, isActive, maxItems) { success, msg ->
                    Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Dialog: Change User Role (via set_user_role RPC ONLY)
    userToChangeRole?.let { user ->
        var selectedRole by remember { mutableStateOf(user.role.lowercase()) }

        AlertDialog(
            onDismissRequest = { userToChangeRole = null },
            title = { Text("Change Role for @${user.effectiveUsername}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select new platform permissions:", fontSize = 13.sp)
                    listOf("user", "creator", "admin").forEach { r ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRole = r }
                        ) {
                            RadioButton(
                                selected = selectedRole == r,
                                onClick = { selectedRole = r },
                                colors = RadioButtonDefaults.colors(selectedColor = QYellowDark)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (r) {
                                    "admin" -> "Admin (Full Control)"
                                    "creator" -> "Creator (Studio Uploads)"
                                    else -> "User (Viewer Only)"
                                },
                                fontSize = 14.sp,
                                color = QOnSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uid = user.id
                        val targetRole = selectedRole
                        userToChangeRole = null
                        viewModel.setUserRole(uid, targetRole) { success, msg ->
                            Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = QYellowDark, contentColor = Color.Black)
                ) {
                    Text("Apply Role")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToChangeRole = null }) { Text("Cancel") }
            },
            containerColor = QSurface,
            titleContentColor = Color.White,
            textContentColor = QOnSurfaceVariant
        )
    }
}

// -----------------------------------------------------------------------------
// Moderation Tab: Series Takedown & Source Reports
// -----------------------------------------------------------------------------
@Composable
private fun ModerationTabView(
    allSeries: List<Series>,
    reports: List<SourceReport>,
    currentFilter: String?,
    onFilterChange: (String?) -> Unit,
    onBlockClick: (Series) -> Unit,
    onUnblockClick: (Series) -> Unit,
    onTogglePublish: (Series) -> Unit,
    onDeleteSeries: (Series) -> Unit,
    onMarkReportHandled: (String) -> Unit,
    onReopenReport: (String) -> Unit,
    onDisableSource: (String) -> Unit,
    onDeleteReport: (String) -> Unit
) {
    var subTab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(QSurface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                label = { Text("Series Catalog (${allSeries.size})") }
            )
            FilterChip(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                label = { Text("Video Reports (${reports.size})") }
            )
        }

        if (subTab == 0) {
            // Series List
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(allSeries, key = { it.id }) { s ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = QSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = s.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = QOnSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (s.isBlocked) Color(0xFFB71C1C) else if (s.isPublished) Color(0xFF2E7D32) else Color(0xFF424242)
                                ) {
                                    Text(
                                        text = if (s.isBlocked) "BLOCKED" else if (s.isPublished) "PUBLISHED" else "DRAFT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (s.isBlocked && !s.blockedReason.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Reason: ${s.blockedReason}", fontSize = 11.sp, color = Color(0xFFFF8A80))
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (s.isBlocked) {
                                    Button(
                                        onClick = { onUnblockClick(s) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Unblock")
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { onBlockClick(s) },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Block", color = Color(0xFFFF8A80))
                                    }
                                }

                                OutlinedButton(
                                    onClick = { onTogglePublish(s) },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (s.isPublished) "Unpublish" else "Publish")
                                }

                                IconButton(onClick = { onDeleteSeries(s) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Source Reports Filter & List
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentFilter == "open",
                        onClick = { onFilterChange("open") },
                        label = { Text("Open") }
                    )
                    FilterChip(
                        selected = currentFilter == "handled",
                        onClick = { onFilterChange("handled") },
                        label = { Text("Handled") }
                    )
                    FilterChip(
                        selected = currentFilter == null,
                        onClick = { onFilterChange(null) },
                        label = { Text("All") }
                    )
                }

                if (reports.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No reports found in this view", color = QOnSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(reports, key = { it.id }) { r ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = QSurface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Reason: ${r.reason.uppercase()}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (r.status == "open") Color(0xFFFF5252) else Color(0xFF81C784)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (r.status == "open") Color.Red.copy(alpha = 0.2f) else Color.Green.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = r.status.uppercase(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    if (!r.details.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(r.details, fontSize = 12.sp, color = QOnSurface)
                                    }

                                    r.source?.let { src ->
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Source: ${src.label} • ${src.url}",
                                            fontSize = 11.sp,
                                            color = QOnSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (r.status == "open") {
                                                Button(
                                                    onClick = { onMarkReportHandled(r.id) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Mark Handled", fontSize = 11.sp)
                                                }
                                            } else {
                                                OutlinedButton(
                                                    onClick = { onReopenReport(r.id) },
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Reopen", fontSize = 11.sp)
                                                }
                                            }

                                            r.source?.let { src ->
                                                OutlinedButton(
                                                    onClick = { onDisableSource(src.id) },
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Disable Source", fontSize = 11.sp, color = Color(0xFFFF8A80))
                                                }
                                            }
                                        }

                                        IconButton(onClick = { onDeleteReport(r.id) }) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Shelves Management Tab
// -----------------------------------------------------------------------------
@Composable
private fun ShelvesTabView(
    shelves: List<Shelf>,
    categories: List<ContentCategory>,
    onAddShelf: () -> Unit,
    onEditShelf: (Shelf) -> Unit,
    onDeleteShelf: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Home Screen Shelves", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = QOnSurface)
            Button(
                onClick = onAddShelf,
                colors = ButtonDefaults.buttonColors(containerColor = QYellowDark, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Shelf", fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(shelves, key = { it.id }) { shelf ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = QSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(shelf.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = QOnSurface)
                            Text("Slug: ${shelf.slug} • Type: ${shelf.type} • Layout: ${shelf.layout}", fontSize = 11.sp, color = QOnSurfaceVariant)
                            Text("Position: ${shelf.position} • Max: ${shelf.maxItems} • Active: ${shelf.isActive}", fontSize = 11.sp, color = QYellowDark)
                        }

                        Row {
                            IconButton(onClick = { onEditShelf(shelf) }) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = QOnSurface)
                            }
                            IconButton(onClick = { onDeleteShelf(shelf.id) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Categories Management Tab
// -----------------------------------------------------------------------------
@Composable
private fun CategoriesTabView(
    categories: List<ContentCategory>,
    onAddCategory: () -> Unit,
    onEditCategory: (ContentCategory) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Content Categories", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = QOnSurface)
            Button(
                onClick = onAddCategory,
                colors = ButtonDefaults.buttonColors(containerColor = QYellowDark, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Category", fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(categories, key = { it.id }) { cat ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = QSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(cat.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = QOnSurface)
                            Text("Slug: ${cat.slug} • Order: ${cat.sortOrder} • Active: ${cat.isActive}", fontSize = 11.sp, color = QOnSurfaceVariant)
                        }

                        Row {
                            IconButton(onClick = { onEditCategory(cat) }) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = QOnSurface)
                            }
                            IconButton(onClick = { onDeleteCategory(cat.id) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Roles Management Tab (set_user_role RPC)
// -----------------------------------------------------------------------------
@Composable
private fun RolesTabView(
    users: List<UserProfile>,
    onSearch: (String) -> Unit,
    onChangeRoleClick: (UserProfile) -> Unit
) {
    var query by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                onSearch(it)
            },
            label = { Text("Search users by username or name") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(users, key = { it.id }) { u ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = QSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = QSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(u.initials, fontWeight = FontWeight.Bold, color = QOnSurface)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(u.effectiveDisplayName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = QOnSurface)
                                Text("@${u.effectiveUsername}", fontSize = 12.sp, color = QOnSurfaceVariant)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (u.role.lowercase()) {
                                    "admin" -> Color(0xFFD32F2F)
                                    "creator" -> QYellowDark
                                    else -> Color(0xFF424242)
                                }
                            ) {
                                Text(
                                    text = u.role.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (u.role.lowercase() == "creator") Color.Black else Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Button(
                                onClick = { onChangeRoleClick(u) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = QSurfaceVariant)
                            ) {
                                Text("Change Role", fontSize = 11.sp, color = QOnSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Category Editor Dialog
// -----------------------------------------------------------------------------
@Composable
private fun CategoryEditorDialog(
    existing: ContentCategory?,
    onDismiss: () -> Unit,
    onSave: (slug: String, name: String, sortOrder: Int, isActive: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var slug by remember { mutableStateOf(existing?.slug ?: "") }
    var sortOrderText by remember { mutableStateOf("${existing?.sortOrder ?: 0}") }
    var isActive by remember { mutableStateOf(existing?.isActive ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Category" else "Edit Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (existing == null) {
                            slug = it.trim().lowercase().replace("[^a-z0-9-]".toRegex(), "-")
                        }
                    },
                    label = { Text("Category Name *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = slug,
                    onValueChange = { slug = it.lowercase() },
                    label = { Text("Slug (lowercase, hyphens only)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sortOrderText,
                    onValueChange = { sortOrderText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Sort Order") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Active", color = Color.White)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && slug.isNotBlank()) {
                        onSave(slug, name, sortOrderText.toIntOrNull() ?: 0, isActive)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = QYellowDark, contentColor = Color.Black)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = QSurface,
        titleContentColor = Color.White,
        textContentColor = QOnSurfaceVariant
    )
}

// -----------------------------------------------------------------------------
// Shelf Editor Dialog
// -----------------------------------------------------------------------------
@Composable
private fun ShelfEditorDialog(
    existing: Shelf?,
    categories: List<ContentCategory>,
    onDismiss: () -> Unit,
    onSave: (slug: String, title: String, type: String, layout: String, config: Map<String, Any?>, categoryId: String?, position: Int, isActive: Boolean, maxItems: Int) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var slug by remember { mutableStateOf(existing?.slug ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: "manual") }
    var layout by remember { mutableStateOf(existing?.layout ?: "row") }
    var configKey by remember { mutableStateOf("") }
    var configVal by remember { mutableStateOf("") }
    var positionText by remember { mutableStateOf("${existing?.position ?: 10}") }
    var maxItemsText by remember { mutableStateOf("${existing?.maxItems ?: 15}") }
    var isActive by remember { mutableStateOf(existing?.isActive ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Shelf" else "Edit Shelf") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (existing == null) slug = it.trim().lowercase().replace("[^a-z0-9-]".toRegex(), "-")
                    },
                    label = { Text("Shelf Title *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = slug,
                    onValueChange = { slug = it.lowercase() },
                    label = { Text("Slug *") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Type: $type (manual, genre, category, auto_trending, latest_episodes, top_rated, hero)", fontSize = 11.sp, color = QOnSurfaceVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("manual", "genre", "category", "auto_trending", "latest_episodes", "top_rated", "hero").forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = {
                                type = t
                                layout = when (t) {
                                    "hero" -> "hero"
                                    "top_rated" -> "top10"
                                    else -> "row"
                                }
                            },
                            label = { Text(t) }
                        )
                    }
                }

                Text("Layout: $layout (row, hero, top10, grid)", fontSize = 11.sp, color = QOnSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("row", "hero", "top10", "grid").forEach { l ->
                        FilterChip(
                            selected = layout == l,
                            onClick = { layout = l },
                            label = { Text(l) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = positionText,
                        onValueChange = { positionText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Position") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = maxItemsText,
                        onValueChange = { maxItemsText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Max Items (1-50)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Active", color = Color.White)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && slug.isNotBlank()) {
                        val pos = positionText.toIntOrNull() ?: 10
                        val maxI = (maxItemsText.toIntOrNull() ?: 15).coerceIn(1, 50)
                        val config = when (type) {
                            "genre" -> mapOf("genre" to "horror")
                            "category" -> mapOf("category" to "movie")
                            "auto_trending" -> mapOf("days" to 7)
                            else -> emptyMap()
                        }
                        onSave(slug, title, type, layout, config, null, pos, isActive, maxI)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = QYellowDark, contentColor = Color.Black)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = QSurface,
        titleContentColor = Color.White,
        textContentColor = QOnSurfaceVariant
    )
}

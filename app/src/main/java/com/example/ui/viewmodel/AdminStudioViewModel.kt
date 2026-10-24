package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.SeriesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AdminTab {
    MODERATION,
    SHELVES,
    CATEGORIES,
    ROLES
}

class AdminStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SeriesRepository(application)

    private val _selectedTab = MutableStateFlow(AdminTab.MODERATION)
    val selectedTab: StateFlow<AdminTab> = _selectedTab.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _allSeries = MutableStateFlow<List<Series>>(emptyList())
    val allSeries: StateFlow<List<Series>> = _allSeries.asStateFlow()

    private val _allCategories = MutableStateFlow<List<ContentCategory>>(emptyList())
    val allCategories: StateFlow<List<ContentCategory>> = _allCategories.asStateFlow()

    private val _allShelves = MutableStateFlow<List<Shelf>>(emptyList())
    val allShelves: StateFlow<List<Shelf>> = _allShelves.asStateFlow()

    private val _selectedShelf = MutableStateFlow<Shelf?>(null)
    val selectedShelf: StateFlow<Shelf?> = _selectedShelf.asStateFlow()

    private val _shelfItems = MutableStateFlow<List<ShelfItem>>(emptyList())
    val shelfItems: StateFlow<List<ShelfItem>> = _shelfItems.asStateFlow()

    private val _sourceReports = MutableStateFlow<List<SourceReport>>(emptyList())
    val sourceReports: StateFlow<List<SourceReport>> = _sourceReports.asStateFlow()

    private val _reportFilter = MutableStateFlow<String?>("open")
    val reportFilter: StateFlow<String?> = _reportFilter.asStateFlow()

    private val _searchedUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val searchedUsers: StateFlow<List<UserProfile>> = _searchedUsers.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    fun setBanner(msg: String?) {
        _bannerMessage.value = msg
    }

    fun selectTab(tab: AdminTab) {
        _selectedTab.value = tab
        when (tab) {
            AdminTab.MODERATION -> {
                loadSeriesAdmin()
                loadReports(_reportFilter.value)
            }
            AdminTab.SHELVES -> loadShelvesAdmin()
            AdminTab.CATEGORIES -> loadCategoriesAdmin()
            AdminTab.ROLES -> {
                if (_searchedUsers.value.isEmpty()) searchUsers("")
            }
        }
    }

    init {
        loadAllData()
    }

    fun loadAllData() {
        viewModelScope.launch {
            _isLoading.value = true
            loadSeriesAdmin()
            loadReports(_reportFilter.value)
            loadShelvesAdmin()
            loadCategoriesAdmin()
            _isLoading.value = false
        }
    }

    // -------------------------------------------------------------------------
    // Moderation (Series & Reports)
    // -------------------------------------------------------------------------
    fun loadSeriesAdmin() {
        viewModelScope.launch {
            val res = repository.listAllSeriesAdmin()
            if (res.isSuccess) {
                _allSeries.value = res.getOrDefault(emptyList())
            }
        }
    }

    fun blockSeries(seriesId: String, reason: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.blockSeries(seriesId, reason)
            if (res.isSuccess) {
                loadSeriesAdmin()
                onDone(true, "Series blocked: $reason")
            } else {
                onDone(false, (res as? SeriesResult.Error)?.message ?: "Failed to block series")
            }
        }
    }

    fun unblockSeries(seriesId: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.unblockSeries(seriesId)
            if (res.isSuccess) {
                loadSeriesAdmin()
                onDone(true, "Series unblocked")
            } else {
                onDone(false, (res as? SeriesResult.Error)?.message ?: "Failed to unblock series")
            }
        }
    }

    fun deleteSeriesAdmin(seriesId: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.deleteSeries(seriesId)
            if (res.isSuccess) {
                loadSeriesAdmin()
                onDone(true, "Series deleted")
            } else {
                onDone(false, (res as? SeriesResult.Error)?.message ?: "Failed to delete series")
            }
        }
    }

    fun togglePublishAdmin(series: Series, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val newPub = !series.isPublished
            val res = repository.updateSeries(series.id, SeriesUpdate(isPublished = newPub))
            if (res.isSuccess) {
                loadSeriesAdmin()
                onDone(true, if (newPub) "Published" else "Unpublished")
            } else {
                onDone(false, (res as? SeriesResult.Error)?.message ?: "Failed to update publish state")
            }
        }
    }

    fun setReportFilter(status: String?) {
        _reportFilter.value = status
        loadReports(status)
    }

    fun loadReports(status: String? = _reportFilter.value) {
        viewModelScope.launch {
            val res = repository.listSourceReports(status)
            if (res.isSuccess) {
                _sourceReports.value = res.getOrDefault(emptyList())
            }
        }
    }

    fun markReportHandled(reportId: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.markReportHandled(reportId, "handled")
            if (res.isSuccess) {
                loadReports(_reportFilter.value)
                onDone(true, "Report marked as handled")
            } else {
                onDone(false, (res as? SeriesResult.Error)?.message ?: "Failed to update report")
            }
        }
    }

    fun reopenReport(reportId: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.markReportHandled(reportId, "open")
            if (res.isSuccess) {
                loadReports(_reportFilter.value)
                onDone(true, "Report reopened")
            } else {
                onDone(false, (res as? SeriesResult.Error)?.message ?: "Failed to reopen report")
            }
        }
    }

    fun disableReportedSource(sourceId: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.disableSource(sourceId)
            if (res.isSuccess) {
                onDone(true, "Source deactivated")
            } else {
                onDone(false, (res as? SeriesResult.Error)?.message ?: "Failed to deactivate source")
            }
        }
    }

    fun deleteReport(reportId: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.deleteSourceReport(reportId)
            if (res.isSuccess) {
                loadReports(_reportFilter.value)
                onDone(true, "Report deleted")
            } else {
                onDone(false, (res as? SeriesResult.Error)?.message ?: "Failed to delete report")
            }
        }
    }

    // -------------------------------------------------------------------------
    // Shelves Management
    // -------------------------------------------------------------------------
    fun loadShelvesAdmin() {
        viewModelScope.launch {
            val res = repository.getShelvesAdmin()
            if (res.isSuccess) {
                _allShelves.value = res.getOrDefault(emptyList())
            }
        }
    }

    fun selectShelf(shelf: Shelf?) {
        _selectedShelf.value = shelf
        if (shelf != null) {
            loadShelfItems(shelf.id)
        } else {
            _shelfItems.value = emptyList()
        }
    }

    fun loadShelfItems(shelfId: String) {
        viewModelScope.launch {
            val res = repository.getShelfItems(shelfId)
            if (res.isSuccess) {
                _shelfItems.value = res.getOrDefault(emptyList())
            }
        }
    }

    fun saveShelf(
        id: String?,
        slug: String,
        title: String,
        type: String,
        layout: String,
        config: Map<String, Any?>,
        categoryId: String?,
        position: Int,
        isActive: Boolean,
        maxItems: Int,
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            if (id.isNullOrBlank()) {
                val insert = ShelfInsert(
                    slug = slug.trim().lowercase(),
                    title = title.trim(),
                    type = type,
                    layout = layout,
                    config = config,
                    categoryId = categoryId,
                    position = position,
                    isActive = isActive,
                    maxItems = maxItems.coerceIn(1, 50)
                )
                val res = repository.createShelf(insert)
                if (res.isSuccess) {
                    loadShelvesAdmin()
                    onDone(true, "Shelf created!")
                } else {
                    onDone(false, (res as? SeriesResult.Error)?.message ?: "Failed to create shelf")
                }
            } else {
                val update = ShelfUpdate(
                    slug = slug.trim().lowercase(),
                    title = title.trim(),
                    type = type,
                    layout = layout,
                    config = config,
                    categoryId = categoryId,
                    position = position,
                    isActive = isActive,
                    maxItems = maxItems.coerceIn(1, 50)
                )
                val res = repository.updateShelf(id, update)
                if (res.isSuccess) {
                    loadShelvesAdmin()
                    onDone(true, "Shelf updated!")
                } else {
                    onDone(false, (res as? SeriesResult.Error)?.message ?: "Failed to update shelf")
                }
            }
        }
    }

    fun deleteShelf(shelfId: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.deleteShelf(shelfId)
            if (res.isSuccess) {
                loadShelvesAdmin()
                if (_selectedShelf.value?.id == shelfId) {
                    _selectedShelf.value = null
                    _shelfItems.value = emptyList()
                }
                onDone(true, "Shelf deleted")
            } else {
                onDone(false, (res as? SeriesResult.Error)?.message ?: "Failed to delete shelf")
            }
        }
    }

    fun addSeriesToShelf(shelfId: String, seriesId: String, position: Int, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.setShelfItem(shelfId, seriesId, position)
            if (res.isSuccess) {
                loadShelfItems(shelfId)
                onDone(true, "Series added to shelf")
            } else {
                onDone(false, (res as? SeriesResult.Error)?.message ?: "Failed to add series to shelf")
            }
        }
    }

    fun removeSeriesFromShelf(shelfId: String, seriesId: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.removeShelfItem(shelfId, seriesId)
            if (res.isSuccess) {
                loadShelfItems(shelfId)
                onDone(true, "Series removed from shelf")
            } else {
                onDone(false, (res as? SeriesResult.Error)?.message ?: "Failed to remove series from shelf")
            }
        }
    }

    // -------------------------------------------------------------------------
    // Categories Management
    // -------------------------------------------------------------------------
    fun loadCategoriesAdmin() {
        viewModelScope.launch {
            val res = repository.getCategoriesAdmin()
            if (res.isSuccess) {
                _allCategories.value = res.getOrDefault(emptyList())
            }
        }
    }

    fun saveCategory(
        id: String?,
        slug: String,
        name: String,
        sortOrder: Int,
        isActive: Boolean,
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            if (id.isNullOrBlank()) {
                val insert = CategoryInsert(
                    slug = slug.trim().lowercase(),
                    name = name.trim(),
                    sortOrder = sortOrder,
                    isActive = isActive
                )
                val res = repository.createCategory(insert)
                if (res.isSuccess) {
                    loadCategoriesAdmin()
                    onDone(true, "Category created!")
                } else {
                    onDone(false, (res as? SeriesResult.Error)?.message ?: "Failed to create category")
                }
            } else {
                val update = CategoryUpdate(
                    slug = slug.trim().lowercase(),
                    name = name.trim(),
                    sortOrder = sortOrder,
                    isActive = isActive
                )
                val res = repository.updateCategory(id, update)
                if (res.isSuccess) {
                    loadCategoriesAdmin()
                    onDone(true, "Category updated!")
                } else {
                    onDone(false, (res as? SeriesResult.Error)?.message ?: "Failed to update category")
                }
            }
        }
    }

    fun deleteCategory(categoryId: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.deleteCategory(categoryId)
            if (res.isSuccess) {
                loadCategoriesAdmin()
                onDone(true, "Category deleted")
            } else {
                onDone(false, (res as? SeriesResult.Error)?.message ?: "Failed to delete category")
            }
        }
    }

    // -------------------------------------------------------------------------
    // Roles & Creator Management (set_user_role RPC)
    // -------------------------------------------------------------------------
    fun searchUsers(query: String) {
        viewModelScope.launch {
            val res = repository.searchUsers(query)
            if (res.isSuccess) {
                _searchedUsers.value = res.getOrDefault(emptyList())
            }
        }
    }

    fun setUserRole(userId: String, newRole: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.setUserRole(userId, newRole)
            if (res.isSuccess) {
                // Refresh searched users list with the updated role
                _searchedUsers.value = _searchedUsers.value.map { u ->
                    if (u.id == userId) u.copy(role = newRole) else u
                }
                onDone(true, "User role changed to '$newRole'")
            } else {
                val error = (res as? SeriesResult.Error)?.message ?: "Failed to update user role"
                onDone(false, error)
            }
        }
    }
}

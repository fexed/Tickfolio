package com.fexed.checklist.ui.screens.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fexed.checklist.data.local.ChecklistDao
import com.fexed.checklist.data.model.ChecklistRecurrence
import com.fexed.checklist.data.model.ChecklistWithItems
import com.fexed.checklist.data.preferences.ChecklistSortOption
import com.fexed.checklist.data.preferences.StartPageOption
import com.fexed.checklist.data.preferences.ThemeMode
import com.fexed.checklist.data.preferences.UserPreferences
import com.fexed.checklist.data.repository.ChecklistRepository
import com.fexed.checklist.util.SampleDataProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OverviewUiState(
    val checklists: List<ChecklistWithItems> = emptyList(),
    val searchQuery: String = "",
    val userPreferences: UserPreferences = UserPreferences(
        startPage = StartPageOption.ALL_CHECKLISTS,
        themeMode = ThemeMode.SYSTEM,
        lastOpenedChecklistId = null
    ),
    val isLoading: Boolean = true
)

class OverviewViewModel(
    private val repository: ChecklistRepository,
    private val checklistDao: ChecklistDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<OverviewUiState> = combine(
        repository.allChecklists,
        repository.userPreferences,
        _searchQuery
    ) { checklists, prefs, query ->
        val filtered = if (query.isBlank()) {
            checklists
        } else {
            val q = query.trim().lowercase()
            checklists.filter { item ->
                item.checklist.title.lowercase().contains(q) ||
                        item.checklist.description.lowercase().contains(q) ||
                        item.items.any {
                            it.title.lowercase().contains(q) ||
                                    it.description.lowercase().contains(q) ||
                                    it.tags.any { tag -> tag.lowercase().contains(q) }
                        }
            }
        }
        OverviewUiState(
            checklists = sortChecklists(
                checklists = filtered,
                option = prefs.checklistSortOption,
                ascending = prefs.checklistSortAscending
            ),
            searchQuery = query,
            userPreferences = prefs,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OverviewUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setChecklistSortOption(option: ChecklistSortOption) {
        viewModelScope.launch {
            repository.setChecklistSortOption(option)
        }
    }

    fun setChecklistSortAscending(ascending: Boolean) {
        viewModelScope.launch {
            repository.setChecklistSortAscending(ascending)
        }
    }

    fun createChecklist(title: String, description: String, iconName: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.createChecklist(title = title, description = description, iconName = iconName)
            onCreated(id)
        }
    }

    fun updateChecklist(id: Long, title: String, description: String, iconName: String) {
        viewModelScope.launch {
            val existing = repository.getChecklistById(id) ?: return@launch
            repository.updateChecklist(existing.copy(title = title, description = description, iconName = iconName))
        }
    }

    fun duplicateChecklist(id: Long) {
        viewModelScope.launch {
            repository.duplicateChecklist(id)
        }
    }

    fun resetChecklist(id: Long) {
        viewModelScope.launch {
            repository.resetAllItems(id)
        }
    }

    fun deleteChecklist(id: Long) {
        viewModelScope.launch {
            repository.deleteChecklist(id)
        }
    }

    fun deleteChecklists(ids: Set<Long>) {
        viewModelScope.launch {
            ids.forEach { repository.deleteChecklist(it) }
        }
    }

    fun updateReminders(
        checklistId: Long,
        reminderTime: Long?,
        geofenceLat: Double?,
        geofenceLng: Double?,
        geofenceRadius: Float,
        geofenceName: String?,
        recurrence: ChecklistRecurrence?,
        recurrenceTimeMinutes: Int,
        recurrenceDayOfWeek: Int,
        recurrenceDayOfMonth: Int
    ) {
        viewModelScope.launch {
            repository.updateChecklistReminders(
                checklistId = checklistId,
                reminderTime = reminderTime,
                geofenceLat = geofenceLat,
                geofenceLng = geofenceLng,
                geofenceRadius = geofenceRadius,
                geofenceName = geofenceName,
                recurrence = recurrence,
                recurrenceTimeMinutes = recurrenceTimeMinutes,
                recurrenceDayOfWeek = recurrenceDayOfWeek,
                recurrenceDayOfMonth = recurrenceDayOfMonth
            )
        }
    }

    fun setStartPage(option: StartPageOption) {
        viewModelScope.launch {
            repository.setStartPage(option)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun setHandwritingFontEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setHandwritingFontEnabled(enabled)
        }
    }

    fun setDriveFolderName(name: String) {
        viewModelScope.launch {
            repository.setDriveFolderName(name)
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            SampleDataProvider.populateSampleData(checklistDao)
        }
    }
}

class OverviewViewModelFactory(
    private val repository: ChecklistRepository,
    private val checklistDao: ChecklistDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return OverviewViewModel(repository, checklistDao) as T
    }
}

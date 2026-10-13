package com.fexed.checklist.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fexed.checklist.data.model.ChecklistItem
import com.fexed.checklist.data.model.ChecklistWithItems
import com.fexed.checklist.data.repository.ChecklistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(
    val checklistWithItems: ChecklistWithItems? = null,
    val selectedTag: String? = null,
    val searchQuery: String = "",
    val hideCompleted: Boolean = false,
    val isLoading: Boolean = true
) {
    val filteredItems: List<ChecklistItem>
        get() {
            val items = checklistWithItems?.items ?: emptyList()
            return items.filter { item ->
                val matchesTag = selectedTag == null || item.tag.equals(selectedTag, ignoreCase = true)
                val matchesSearch = searchQuery.isBlank() ||
                        item.title.contains(searchQuery, ignoreCase = true) ||
                        item.description.contains(searchQuery, ignoreCase = true) ||
                        item.tag.contains(searchQuery, ignoreCase = true)
                val matchesHideCompleted = !hideCompleted || !item.isCompleted
                matchesTag && matchesSearch && matchesHideCompleted
            }
        }
}

class ChecklistDetailViewModel(
    private val checklistId: Long,
    private val repository: ChecklistRepository
) : ViewModel() {

    private val _selectedTag = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _hideCompleted = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            repository.markChecklistOpened(checklistId)
        }
    }

    val uiState: StateFlow<DetailUiState> = combine(
        repository.getChecklist(checklistId),
        _selectedTag,
        _searchQuery,
        _hideCompleted
    ) { checklistWithItems, tag, query, hideComp ->
        DetailUiState(
            checklistWithItems = checklistWithItems,
            selectedTag = tag,
            searchQuery = query,
            hideCompleted = hideComp,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DetailUiState()
    )

    fun onTagSelected(tag: String?) {
        _selectedTag.value = tag
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleHideCompleted() {
        _hideCompleted.value = !_hideCompleted.value
    }

    fun setItemCheckCount(id: Long, completedChecks: Int) {
        viewModelScope.launch {
            repository.setItemCheckCount(id, completedChecks)
        }
    }

    fun addItem(title: String, description: String, tag: String, imageUri: String?, requiredChecks: Int) {
        viewModelScope.launch {
            repository.addItem(checklistId, title, description, tag, imageUri, requiredChecks)
        }
    }

    fun updateItem(item: ChecklistItem, title: String, description: String, tag: String, imageUri: String?, requiredChecks: Int) {
        viewModelScope.launch {
            val completedChecks = item.completedChecks.coerceIn(0, requiredChecks)
            repository.updateItem(
                item.copy(
                    title = title,
                    description = description,
                    tag = tag,
                    imageUri = imageUri,
                    isCompleted = completedChecks >= requiredChecks,
                    requiredChecks = requiredChecks,
                    completedChecks = completedChecks
                )
            )
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            repository.deleteItem(id)
        }
    }

    fun resetAllItems() {
        viewModelScope.launch {
            repository.resetAllItems(checklistId)
        }
    }

    fun completeAllItems() {
        viewModelScope.launch {
            repository.completeAllItems(checklistId)
        }
    }

    fun updateChecklist(title: String, description: String, iconName: String) {
        viewModelScope.launch {
            val current = uiState.value.checklistWithItems?.checklist ?: return@launch
            repository.updateChecklist(current.copy(title = title, description = description, iconName = iconName))
        }
    }

    fun updateReminders(
        reminderTime: Long?,
        geofenceLat: Double?,
        geofenceLng: Double?,
        geofenceRadius: Float,
        geofenceName: String?
    ) {
        viewModelScope.launch {
            val current = uiState.value.checklistWithItems?.checklist ?: return@launch
            repository.updateChecklist(
                current.copy(
                    reminderTime = reminderTime,
                    geofenceLat = geofenceLat,
                    geofenceLng = geofenceLng,
                    geofenceRadius = geofenceRadius,
                    geofenceName = geofenceName
                )
            )
        }
    }

    fun deleteChecklist(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteChecklist(checklistId)
            onDeleted()
        }
    }
}

class ChecklistDetailViewModelFactory(
    private val checklistId: Long,
    private val repository: ChecklistRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChecklistDetailViewModel(checklistId, repository) as T
    }
}

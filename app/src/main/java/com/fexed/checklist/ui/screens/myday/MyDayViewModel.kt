package com.fexed.checklist.ui.screens.myday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fexed.checklist.data.repository.ChecklistRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MyDayUiState(
    val today: LocalDate = LocalDate.now(),
    val tasks: List<MyDayTask> = emptyList(),
    val availableItems: List<MyDayTask> = emptyList(),
    val isLoading: Boolean = true
)

class MyDayViewModel(
    private val repository: ChecklistRepository
) : ViewModel() {
    private val clock = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(CLOCK_REFRESH_MILLIS)
        }
    }

    val uiState = combine(repository.allChecklists, repository.myDaySelection, clock) {
            checklists,
            selection,
            now ->
        val today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
        val selectedIds = if (selection.dateKey == today.toString()) {
            selection.itemSyncIds
        } else {
            emptySet()
        }
        val content = MyDayTaskFactory.create(
            checklists = checklists,
            selectedItemSyncIds = selectedIds,
            nowMillis = now
        )
        MyDayUiState(
            today = today,
            tasks = content.tasks,
            availableItems = content.availableItems,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MyDayUiState()
    )

    fun toggleSelection(itemSyncId: String) {
        viewModelScope.launch {
            repository.toggleMyDayItemSelection(LocalDate.now(ZoneId.systemDefault()), itemSyncId)
        }
    }

    fun setItemCompleted(task: MyDayTask, completed: Boolean) {
        viewModelScope.launch {
            val today = LocalDate.now(ZoneId.systemDefault())
            repository.setMyDayItemSelected(
                date = today,
                itemSyncId = task.item.syncId,
                selected = true
            )
            repository.toggleItemCompleted(task.item.id, completed)
        }
    }

    companion object {
        private const val CLOCK_REFRESH_MILLIS = 60_000L
    }
}

class MyDayViewModelFactory(
    private val repository: ChecklistRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MyDayViewModel(repository) as T
    }
}

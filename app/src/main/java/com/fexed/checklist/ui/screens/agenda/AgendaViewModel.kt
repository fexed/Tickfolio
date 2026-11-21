package com.fexed.checklist.ui.screens.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fexed.checklist.data.repository.ChecklistRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class AgendaUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val today: LocalDate = LocalDate.now(),
    val nowMillis: Long = System.currentTimeMillis(),
    val events: List<AgendaEvent> = emptyList(),
    val isLoading: Boolean = true
)

class AgendaViewModel(
    repository: ChecklistRepository
) : ViewModel() {
    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val clock = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(CLOCK_REFRESH_MILLIS)
        }
    }

    val uiState = combine(repository.allChecklists, selectedDate, clock) { checklists, date, now ->
        AgendaUiState(
            selectedDate = date,
            today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate(),
            nowMillis = now,
            events = AgendaEventFactory.create(checklists, now),
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AgendaUiState()
    )

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    companion object {
        private const val CLOCK_REFRESH_MILLIS = 60_000L
    }
}

class AgendaViewModelFactory(
    private val repository: ChecklistRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AgendaViewModel(repository) as T
    }
}

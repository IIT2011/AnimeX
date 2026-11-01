package com.animex.app.feature.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animex.app.core.network.NetworkClient
import com.animex.app.core.network.model.ScheduleDayDto
import com.animex.app.core.network.model.ScheduleDto
import com.animex.app.core.network.model.ScheduleItemDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ScheduleUiState {
    data object Loading : ScheduleUiState
    data class Success(
        val schedule: ScheduleDto,
        val selectedDay: ScheduleDayDto? = null,
        val filteredItems: List<ScheduleItemDto> = emptyList()
    ) : ScheduleUiState
    data class Error(val message: String) : ScheduleUiState
}

class ScheduleViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ScheduleUiState>(ScheduleUiState.Loading)
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        loadSchedule()
    }

    fun loadSchedule(date: String? = null) {
        viewModelScope.launch {
            _uiState.value = ScheduleUiState.Loading
            try {
                val schedule = NetworkClient.api.getSchedule(date)
                val currentDay = schedule.days.firstOrNull { it.date == schedule.date }
                    ?: schedule.days.firstOrNull()

                _uiState.value = ScheduleUiState.Success(
                    schedule = schedule,
                    selectedDay = currentDay,
                    filteredItems = schedule.items
                )
            } catch (e: Exception) {
                _uiState.value = ScheduleUiState.Error(e.localizedMessage ?: "Failed to load broadcast timings")
            }
        }
    }

    fun selectDay(day: ScheduleDayDto) {
        val current = _uiState.value
        if (current is ScheduleUiState.Success) {
            _uiState.value = current.copy(selectedDay = day)
            // Reload schedule for the chosen date
            loadSchedule(day.date)
        }
    }
}

package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.TripRepository
import com.example.data.TripVisitEntity
import com.example.util.DateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TripTab {
    BERANDA,
    KALENDER,
    KENANGAN
}

enum class MemoryFilter {
    SEMUA,
    DENGAN_CATATAN
}

private data class NavAndModalState(
    val selectedTab: TripTab = TripTab.BERANDA,
    val currentYear: Int = DateUtils.currentYearAndMonth().first,
    val currentMonth: Int = DateUtils.currentYearAndMonth().second,
    val selectedDateEpochDay: Long = DateUtils.todayEpochDay(),
    val memoryFilter: MemoryFilter = MemoryFilter.SEMUA,
    val memorySearchQuery: String = "",
    val activeMapsVisit: TripVisitEntity? = null,
    val activeAddEditDateEpochDay: Long? = null,
    val editingVisit: TripVisitEntity? = null,
    val activeMemoryDetailVisit: TripVisitEntity? = null
)

data class TripBuddyUiState(
    val selectedTab: TripTab = TripTab.BERANDA,
    val currentYear: Int = DateUtils.currentYearAndMonth().first,
    val currentMonth: Int = DateUtils.currentYearAndMonth().second,
    val selectedDateEpochDay: Long = DateUtils.todayEpochDay(),
    val todayEpochDay: Long = DateUtils.todayEpochDay(),
    val nowMillis: Long = System.currentTimeMillis(),
    val allVisits: List<TripVisitEntity> = emptyList(),
    val upcomingVisits: List<TripVisitEntity> = emptyList(),
    val nearestTrip: TripVisitEntity? = null,
    val visitedMemories: List<TripVisitEntity> = emptyList(),
    val filteredMemories: List<TripVisitEntity> = emptyList(),
    val memoryFilter: MemoryFilter = MemoryFilter.SEMUA,
    val memorySearchQuery: String = "",
    val visitsOnSelectedDate: List<TripVisitEntity> = emptyList(),
    val upcomingDatesInMonth: Set<Long> = emptySet(),
    val memoryDatesInMonth: Set<Long> = emptySet(),
    val activeMapsVisit: TripVisitEntity? = null,
    val activeAddEditDateEpochDay: Long? = null,
    val editingVisit: TripVisitEntity? = null,
    val activeMemoryDetailVisit: TripVisitEntity? = null
)

class TripBuddyViewModel(private val repository: TripRepository) : ViewModel() {

    private val navAndModalState = MutableStateFlow(NavAndModalState())

    private val tickerMillis = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1_000L)
        }
    }

    val uiState: StateFlow<TripBuddyUiState> = combine(
        repository.allVisits,
        navAndModalState,
        tickerMillis
    ) { visits, nav, nowMillis ->
        val today = DateUtils.todayEpochDay(nowMillis)

        val upcoming = visits.filter { visit ->
            !visit.isCompletedManually && visit.visitDateEpochDay >= today
        }

        val memories = visits
            .filter { visit ->
                visit.isCompletedManually || visit.visitDateEpochDay < today
            }
            .sortedWith(
                compareByDescending<TripVisitEntity> { it.visitDateEpochDay }
                    .thenByDescending { it.visitHour }
                    .thenByDescending { it.visitMinute }
                    .thenByDescending { it.createdAtMillis }
            )

        val queryLower = nav.memorySearchQuery.trim().lowercase()
        val filteredMemories = memories.filter { visit ->
            val matchesFilter = when (nav.memoryFilter) {
                MemoryFilter.SEMUA -> true
                MemoryFilter.DENGAN_CATATAN -> visit.memoryNotes.isNotBlank()
            }
            val matchesSearch = if (queryLower.isEmpty()) {
                true
            } else {
                visit.locationName.lowercase().contains(queryLower) ||
                    visit.tripTitle.lowercase().contains(queryLower) ||
                    visit.memoryNotes.lowercase().contains(queryLower)
            }
            matchesFilter && matchesSearch
        }

        val selectedVisits = visits.filter { it.visitDateEpochDay == nav.selectedDateEpochDay }

        val daysInActiveMonth = DateUtils.daysInMonth(nav.currentYear, nav.currentMonth)
        val monthStart = DateUtils.epochDayFromYmd(nav.currentYear, nav.currentMonth, 1)
        val monthEnd = DateUtils.epochDayFromYmd(nav.currentYear, nav.currentMonth, daysInActiveMonth)

        val upcomingDates = mutableSetOf<Long>()
        val memoryDates = mutableSetOf<Long>()
        visits.forEach { visit ->
            if (visit.visitDateEpochDay in monthStart..monthEnd) {
                if (!visit.isCompletedManually && visit.visitDateEpochDay >= today) {
                    upcomingDates.add(visit.visitDateEpochDay)
                } else {
                    memoryDates.add(visit.visitDateEpochDay)
                }
            }
        }

        val refreshedMapsVisit = nav.activeMapsVisit?.let { current ->
            visits.find { it.id == current.id } ?: current
        }
        val refreshedMemoryVisit = nav.activeMemoryDetailVisit?.let { current ->
            visits.find { it.id == current.id } ?: current
        }

        TripBuddyUiState(
            selectedTab = nav.selectedTab,
            currentYear = nav.currentYear,
            currentMonth = nav.currentMonth,
            selectedDateEpochDay = nav.selectedDateEpochDay,
            todayEpochDay = today,
            nowMillis = nowMillis,
            allVisits = visits,
            upcomingVisits = upcoming,
            nearestTrip = upcoming.firstOrNull(),
            visitedMemories = memories,
            filteredMemories = filteredMemories,
            memoryFilter = nav.memoryFilter,
            memorySearchQuery = nav.memorySearchQuery,
            visitsOnSelectedDate = selectedVisits,
            upcomingDatesInMonth = upcomingDates,
            memoryDatesInMonth = memoryDates,
            activeMapsVisit = refreshedMapsVisit,
            activeAddEditDateEpochDay = nav.activeAddEditDateEpochDay,
            editingVisit = nav.editingVisit,
            activeMemoryDetailVisit = refreshedMemoryVisit
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TripBuddyUiState()
    )

    fun selectTab(tab: TripTab) {
        navAndModalState.update { it.copy(selectedTab = tab) }
    }

    fun goToPreviousMonth() {
        navAndModalState.update { state ->
            if (state.currentMonth == 0) {
                state.copy(currentYear = state.currentYear - 1, currentMonth = 11)
            } else {
                state.copy(currentMonth = state.currentMonth - 1)
            }
        }
    }

    fun goToNextMonth() {
        navAndModalState.update { state ->
            if (state.currentMonth == 11) {
                state.copy(currentYear = state.currentYear + 1, currentMonth = 0)
            } else {
                state.copy(currentMonth = state.currentMonth + 1)
            }
        }
    }

    fun jumpToToday() {
        val (year, month) = DateUtils.currentYearAndMonth()
        val today = DateUtils.todayEpochDay()
        navAndModalState.update {
            it.copy(currentYear = year, currentMonth = month, selectedDateEpochDay = today)
        }
    }

    fun onCalendarDateClicked(epochDay: Long) {
        val visitsForDate = uiState.value.allVisits.filter { it.visitDateEpochDay == epochDay }
        val (year, month, _) = DateUtils.ymdFromEpochDay(epochDay)
        navAndModalState.update { state ->
            val shouldOpenAddDirectly = visitsForDate.isEmpty() || state.selectedDateEpochDay == epochDay
            state.copy(
                currentYear = year,
                currentMonth = month,
                selectedDateEpochDay = epochDay,
                activeAddEditDateEpochDay = if (shouldOpenAddDirectly) epochDay else null,
                editingVisit = null
            )
        }
    }

    fun openAddLocationSheet(epochDay: Long = navAndModalState.value.selectedDateEpochDay) {
        navAndModalState.update {
            it.copy(
                selectedDateEpochDay = epochDay,
                activeAddEditDateEpochDay = epochDay,
                editingVisit = null
            )
        }
    }

    fun openEditLocationSheet(visit: TripVisitEntity) {
        navAndModalState.update {
            it.copy(
                activeAddEditDateEpochDay = visit.visitDateEpochDay,
                editingVisit = visit
            )
        }
    }

    fun dismissAddEditSheet() {
        navAndModalState.update {
            it.copy(activeAddEditDateEpochDay = null, editingVisit = null)
        }
    }

    fun openMapsSearchSheet(visit: TripVisitEntity) {
        navAndModalState.update { it.copy(activeMapsVisit = visit) }
    }

    fun dismissMapsSearchSheet() {
        navAndModalState.update { it.copy(activeMapsVisit = null) }
    }

    fun openMemoryDetailSheet(visit: TripVisitEntity) {
        navAndModalState.update { it.copy(activeMemoryDetailVisit = visit) }
    }

    fun dismissMemoryDetailSheet() {
        navAndModalState.update { it.copy(activeMemoryDetailVisit = null) }
    }

    fun setMemoryFilter(filter: MemoryFilter) {
        navAndModalState.update { it.copy(memoryFilter = filter) }
    }

    fun setMemorySearchQuery(query: String) {
        navAndModalState.update { it.copy(memorySearchQuery = query) }
    }

    fun saveLocationVisit(
        locationName: String,
        tripTitle: String,
        epochDay: Long,
        hour: Int,
        minute: Int,
        planNotes: String
    ) {
        val cleanLocation = locationName.trim()
        if (cleanLocation.isEmpty()) return

        val existing = navAndModalState.value.editingVisit
        viewModelScope.launch {
            if (existing != null) {
                repository.updateVisit(
                    existing.copy(
                        locationName = cleanLocation,
                        tripTitle = tripTitle.trim(),
                        visitDateEpochDay = epochDay,
                        visitHour = hour,
                        visitMinute = minute,
                        planNotes = planNotes.trim()
                    )
                )
            } else {
                repository.insertVisit(
                    TripVisitEntity(
                        locationName = cleanLocation,
                        tripTitle = tripTitle.trim(),
                        visitDateEpochDay = epochDay,
                        visitHour = hour,
                        visitMinute = minute,
                        planNotes = planNotes.trim()
                    )
                )
            }
            dismissAddEditSheet()
        }
    }

    fun updateLocationNameFromMapsSheet(visit: TripVisitEntity, newLocationName: String) {
        val clean = newLocationName.trim()
        if (clean.isEmpty() || clean == visit.locationName) return
        viewModelScope.launch {
            repository.updateVisit(visit.copy(locationName = clean))
        }
    }

    fun toggleManualCompleted(visit: TripVisitEntity) {
        viewModelScope.launch {
            repository.updateVisit(
                visit.copy(isCompletedManually = !visit.isCompletedManually)
            )
        }
    }

    fun saveMemoryNotes(visit: TripVisitEntity, memoryNotes: String) {
        viewModelScope.launch {
            repository.updateVisit(visit.copy(memoryNotes = memoryNotes.trim()))
            dismissMemoryDetailSheet()
        }
    }

    fun deleteVisit(visit: TripVisitEntity) {
        viewModelScope.launch {
            repository.deleteVisitById(visit.id)
            navAndModalState.update { state ->
                state.copy(
                    activeMapsVisit = if (state.activeMapsVisit?.id == visit.id) null else state.activeMapsVisit,
                    activeMemoryDetailVisit = if (state.activeMemoryDetailVisit?.id == visit.id) null else state.activeMemoryDetailVisit
                )
            }
        }
    }

    class Factory(private val repository: TripRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TripBuddyViewModel(repository) as T
        }
    }
}

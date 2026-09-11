package com.example.shiftly.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shiftly.data.local.AppDatabase
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.model.Expense
import com.example.shiftly.data.model.Shift
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

class ShiftViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val shiftDao = db.shiftDao()

    val shifts: StateFlow<List<Shift>> = shiftDao.getAllShiftsFlow()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Configurable hourly rate in NIS (₪)
    private val _hourlyRate = MutableStateFlow(45.0)
    val hourlyRate: StateFlow<Double> = _hourlyRate.asStateFlow()

    // Default available job roles
    val availableJobRoles = listOf("מלצרות", "בר / מזיגה", "מטבח", "אבטחה", "משרד / ניהול", "שליחויות", "שירות לקוחות", "כללי")

    // Active Shift tracking
    private val _activeShift = MutableStateFlow<Shift?>(null)
    val activeShift: StateFlow<Shift?> = _activeShift.asStateFlow()

    private val _isBreakActive = MutableStateFlow(false)
    val isBreakActive: StateFlow<Boolean> = _isBreakActive.asStateFlow()

    private var breakStartTimestamp: Long? = null
    private var accumulatedBreakMillis: Long = 0L

    // Live updating elapsed clock (updated every second)
    private val _currentElapsedSeconds = MutableStateFlow(0L)
    val currentElapsedSeconds: StateFlow<Long> = _currentElapsedSeconds.asStateFlow()

    private val _currentBreakSeconds = MutableStateFlow(0L)
    val currentBreakSeconds: StateFlow<Long> = _currentBreakSeconds.asStateFlow()

    // Shift currently in Review Sheet
    private val _reviewingShift = MutableStateFlow<Shift?>(null)
    val reviewingShift: StateFlow<Shift?> = _reviewingShift.asStateFlow()

    private var tickerJob: Job? = null

    init {
        // Pre-populate sample shifts if database is empty on first launch
        viewModelScope.launch {
            val existing = shiftDao.getAllShifts()
            if (existing.isEmpty()) {
                prepopulateSampleShifts()
            }
        }
    }

    fun setHourlyRate(rate: Double) {
        if (rate > 0) {
            _hourlyRate.value = rate
            // Also update active shift hourly rate if exists
            val active = _activeShift.value
            if (active != null) {
                _activeShift.value = active.copy(hourlyRate = rate)
            }
        }
    }

    fun startShift(jobRole: String = "מלצרות", isBreakPaid: Boolean = false) {
        val now = System.currentTimeMillis()
        val shift = Shift(
            id = UUID.randomUUID().toString(),
            startTime = now,
            endTime = null,
            breakDurationMinutes = 0,
            isBreakPaid = isBreakPaid,
            jobRole = jobRole,
            hourlyRate = _hourlyRate.value
        )
        _activeShift.value = shift
        _isBreakActive.value = false
        breakStartTimestamp = null
        accumulatedBreakMillis = 0L
        _currentElapsedSeconds.value = 0L
        _currentBreakSeconds.value = 0L

        startTicker()
    }

    fun toggleBreakType(isPaid: Boolean) {
        val active = _activeShift.value ?: return
        _activeShift.value = active.copy(isBreakPaid = isPaid)
    }

    fun pauseForBreak() {
        if (_activeShift.value == null || _isBreakActive.value) return
        _isBreakActive.value = true
        breakStartTimestamp = System.currentTimeMillis()
    }

    fun resumeFromBreak() {
        if (_activeShift.value == null || !_isBreakActive.value) return
        val start = breakStartTimestamp
        if (start != null) {
            accumulatedBreakMillis += (System.currentTimeMillis() - start)
        }
        breakStartTimestamp = null
        _isBreakActive.value = false
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                val active = _activeShift.value
                if (active == null) break

                val now = System.currentTimeMillis()
                val totalElapsedMs = (now - active.startTime).coerceAtLeast(0L)
                _currentElapsedSeconds.value = totalElapsedMs / 1000L

                var currentBreakMs = accumulatedBreakMillis
                if (_isBreakActive.value && breakStartTimestamp != null) {
                    currentBreakMs += (now - breakStartTimestamp!!)
                }
                _currentBreakSeconds.value = currentBreakMs / 1000L

                delay(1000L)
            }
        }
    }

    fun stopShiftToReview() {
        val active = _activeShift.value ?: return
        val now = System.currentTimeMillis()
        tickerJob?.cancel()

        var totalBreakMs = accumulatedBreakMillis
        if (_isBreakActive.value && breakStartTimestamp != null) {
            totalBreakMs += (now - breakStartTimestamp!!)
        }
        val breakMinutes = (totalBreakMs / 60000L).toInt()

        val completedShift = active.copy(
            endTime = now,
            breakDurationMinutes = breakMinutes
        )

        _reviewingShift.value = completedShift
        _activeShift.value = null
        _isBreakActive.value = false
        breakStartTimestamp = null
        accumulatedBreakMillis = 0L
    }

    fun openShiftForReview(shift: Shift) {
        _reviewingShift.value = shift
    }

    fun dismissReviewSheet() {
        _reviewingShift.value = null
    }

    fun saveShift(shift: Shift) {
        viewModelScope.launch {
            shiftDao.insertShift(ShiftEntity.fromDomain(shift))
            _reviewingShift.value = null
        }
    }

    fun discardActiveShift() {
        tickerJob?.cancel()
        _activeShift.value = null
        _isBreakActive.value = false
        breakStartTimestamp = null
        accumulatedBreakMillis = 0L
        _currentElapsedSeconds.value = 0L
        _currentBreakSeconds.value = 0L
        _reviewingShift.value = null
    }

    fun deleteShift(id: String) {
        viewModelScope.launch {
            shiftDao.deleteShiftById(id)
        }
    }

    fun updateShift(shift: Shift) {
        viewModelScope.launch {
            shiftDao.updateShift(ShiftEntity.fromDomain(shift))
        }
    }

    fun addParsedShifts(newShifts: List<Shift>) {
        viewModelScope.launch {
            shiftDao.insertShifts(newShifts.map { ShiftEntity.fromDomain(it) })
        }
    }

    private suspend fun prepopulateSampleShifts() {
        val rate = _hourlyRate.value
        val sampleList = mutableListOf<Shift>()

        // 1. Sept 7, 2026 - 09:00 to 17:00, 45m break, 50 tips
        val cal1Start = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 7, 9, 0, 0)
        }
        val cal1End = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 7, 17, 0, 0)
        }
        sampleList.add(
            Shift(
                startTime = cal1Start.timeInMillis,
                endTime = cal1End.timeInMillis,
                breakDurationMinutes = 45,
                isBreakPaid = false,
                tips = 50.0,
                jobRole = "מלצרות",
                hourlyRate = rate,
                notes = "משמרת בוקר עמוסה"
            )
        )

        // 2. Sept 7-8, 2026 - 22:00 to 06:00, 0m break, 20.5 tips, travel expense 30
        val cal2Start = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 7, 22, 0, 0)
        }
        val cal2End = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 8, 6, 0, 0)
        }
        sampleList.add(
            Shift(
                startTime = cal2Start.timeInMillis,
                endTime = cal2End.timeInMillis,
                breakDurationMinutes = 0,
                isBreakPaid = false,
                tips = 20.5,
                jobRole = "בר / מזיגה",
                hourlyRate = rate,
                expenses = listOf(
                    Expense(title = "מונית לילה חזור", amount = 35.0, category = "נסיעות")
                ),
                notes = "משמרת לילה חמה"
            )
        )

        // 3. Sept 10, 2026 - 12:00 to 20:00, 30m paid break, 75 tips
        val cal3Start = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 10, 12, 0, 0)
        }
        val cal3End = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 10, 20, 0, 0)
        }
        sampleList.add(
            Shift(
                startTime = cal3Start.timeInMillis,
                endTime = cal3End.timeInMillis,
                breakDurationMinutes = 30,
                isBreakPaid = true,
                tips = 75.0,
                jobRole = "מלצרות",
                hourlyRate = rate,
                expenses = listOf(
                    Expense(title = "ארוחת צהריים", amount = 28.0, category = "אוכל")
                ),
                notes = "אמצע שבוע מעולה"
            )
        )

        shiftDao.insertShifts(sampleList.map { ShiftEntity.fromDomain(it) })
    }
}

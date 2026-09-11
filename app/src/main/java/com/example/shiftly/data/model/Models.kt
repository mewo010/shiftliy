package com.example.shiftly.data.model

import java.util.UUID

data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val category: String = "כללי" // נסיעות, אוכל, ציוד, אחר
)

data class Shift(
    val id: String = UUID.randomUUID().toString(),
    val startTime: Long, // Epoch milliseconds
    val endTime: Long? = null, // Epoch milliseconds (null if ongoing)
    val breakDurationMinutes: Int = 0,
    val isBreakPaid: Boolean = false,
    val expenses: List<Expense> = emptyList(),
    val tips: Double = 0.0,
    val jobRole: String = "ברירת מחדל",
    val notes: String = "",
    val hourlyRate: Double = 45.0
) {
    /**
     * Exact elapsed duration in milliseconds.
     */
    val durationMillis: Long
        get() {
            val end = endTime ?: System.currentTimeMillis()
            return (end - startTime).coerceAtLeast(0L)
        }

    val durationSeconds: Long
        get() = durationMillis / 1000L

    val durationMinutes: Long
        get() = durationMillis / 60000L

    /**
     * Unpaid break duration in minutes.
     * Returns 0 if isBreakPaid is true, else breakDurationMinutes.
     */
    val unpaidBreakDuration: Int
        get() = if (isBreakPaid) 0 else breakDurationMinutes

    /**
     * Net duration in hours: total duration minus unpaid breaks in hours.
     */
    val netDurationHours: Double
        get() {
            val totalMinutes = durationMinutes - unpaidBreakDuration
            return (totalMinutes.coerceAtLeast(0L) / 60.0)
        }

    /**
     * Gross earnings based on hourly rate.
     */
    val grossEarnings: Double
        get() = netDurationHours * hourlyRate

    /**
     * Total expenses logged for this shift.
     */
    val totalExpenses: Double
        get() = expenses.sumOf { it.amount }

    /**
     * Net pay = gross earnings + tips - expenses.
     */
    val netPay: Double
        get() = grossEarnings + tips - totalExpenses
}

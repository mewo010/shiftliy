package com.example.shiftly.data.parser

import com.example.shiftly.data.model.Shift
import java.util.Calendar
import java.util.TimeZone

object SmartParser {

    private const val DEFAULT_YEAR = 2026

    /**
     * Regex matching patterns like:
     * 07.09.2026 - 09:00 - 17:00 45 דקות + 50
     * 07.09 - 22:00 - 06:00 ללא + 20.5
     * 15/09/2026 10:00 - 18:30 ללא הפסקה + 30
     */
    private val lineRegex = Regex(
        """(?x)
        # Date: DD.MM or DD.MM.YYYY (or with /)
        (?<day>\d{1,2})[./](?<month>\d{1,2})(?:[./](?<year>\d{2,4}))?
        \s*[-–—:\s]\s*
        # Start Time: HH:mm
        (?<startH>\d{1,2}):(?<startM>\d{2})
        \s*[-–—:\s]\s*
        # End Time: HH:mm
        (?<endH>\d{1,2}):(?<endM>\d{2})
        # Remainder for break & tips
        (?<remainder>.*)
        """.trimIndent()
    )

    private val breakRegex = Regex("""(\d+)\s*(?:דקות|דק['״]|min|minutes)?""", RegexOption.IGNORE_CASE)
    private val noBreakRegex = Regex("""(?:ללא(?:\s+הפסקה)?|none|\b0\b|בלי)""", RegexOption.IGNORE_CASE)
    private val tipRegex = Regex("""(?:\+|\bטיפ(?:ים)?\b)\s*([0-9]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)

    fun parseLine(rawLine: String, defaultHourlyRate: Double = 45.0): Shift? {
        val trimmed = rawLine.trim()
        if (trimmed.isEmpty()) return null

        val match = lineRegex.find(trimmed) ?: return null

        val day = match.groups["day"]?.value?.toIntOrNull() ?: return null
        val month = match.groups["month"]?.value?.toIntOrNull() ?: return null
        var year = match.groups["year"]?.value?.toIntOrNull() ?: DEFAULT_YEAR
        if (year < 100) {
            year += 2000
        }

        val startH = match.groups["startH"]?.value?.toIntOrNull() ?: return null
        val startM = match.groups["startM"]?.value?.toIntOrNull() ?: return null
        val endH = match.groups["endH"]?.value?.toIntOrNull() ?: return null
        val endM = match.groups["endM"]?.value?.toIntOrNull() ?: return null

        val tz = TimeZone.getDefault()
        val startCal = Calendar.getInstance(tz).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, startH)
            set(Calendar.MINUTE, startM)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCal = Calendar.getInstance(tz).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, endH)
            set(Calendar.MINUTE, endM)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Night shift wrap-around: if end is before or equal to start, advance end date by 1 day
        if (endCal.timeInMillis <= startCal.timeInMillis) {
            endCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        val remainder = match.groups["remainder"]?.value.orEmpty().trim()
        var breakMinutes = 0
        var tips = 0.0

        if (remainder.isNotEmpty()) {
            // Check for tips first
            val tipMatch = tipRegex.find(remainder)
            if (tipMatch != null) {
                tips = tipMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            }

            // Check break
            if (noBreakRegex.containsMatchIn(remainder)) {
                breakMinutes = 0
            } else {
                val breakMatch = breakRegex.find(remainder)
                if (breakMatch != null) {
                    breakMinutes = breakMatch.groupValues[1].toIntOrNull() ?: 0
                }
            }
        }

        return Shift(
            startTime = startCal.timeInMillis,
            endTime = endCal.timeInMillis,
            breakDurationMinutes = breakMinutes,
            isBreakPaid = false,
            tips = tips,
            hourlyRate = defaultHourlyRate,
            jobRole = "משמרת",
            notes = "יובא אוטומטית מ-WhatsApp"
        )
    }

    fun parseMultiple(text: String, defaultHourlyRate: Double = 45.0): List<Shift> {
        val results = mutableListOf<Shift>()
        val lines = text.split("\n", ";", "\r\n")
        for (line in lines) {
            val parsed = parseLine(line, defaultHourlyRate)
            if (parsed != null) {
                results.add(parsed)
            }
        }
        return results
    }
}

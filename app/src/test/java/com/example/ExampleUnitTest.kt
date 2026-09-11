package com.example

import com.example.shiftly.data.model.Expense
import com.example.shiftly.data.model.Shift
import com.example.shiftly.data.parser.SmartParser
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {
    @Test
    fun testSmartParserRegularShift() {
        val line = "07.09.2026 - 09:00 - 17:00 45 דקות + 50"
        val shift = SmartParser.parseLine(line, 45.0)
        assertNotNull(shift)

        val calStart = Calendar.getInstance().apply { timeInMillis = shift!!.startTime }
        val calEnd = Calendar.getInstance().apply { timeInMillis = shift!!.endTime!! }

        assertEquals(2026, calStart.get(Calendar.YEAR))
        assertEquals(Calendar.SEPTEMBER, calStart.get(Calendar.MONTH))
        assertEquals(7, calStart.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, calStart.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calStart.get(Calendar.MINUTE))

        assertEquals(7, calEnd.get(Calendar.DAY_OF_MONTH))
        assertEquals(17, calEnd.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calEnd.get(Calendar.MINUTE))

        assertEquals(45, shift!!.breakDurationMinutes)
        assertEquals(50.0, shift.tips, 0.001)

        // 8 hours gross - 45 min unpaid break = 7.25 net hours
        assertEquals(7.25, shift.netDurationHours, 0.05)
    }

    @Test
    fun testSmartParserNightShiftCrossMidnight() {
        val line = "07.09 - 22:00 - 06:00 ללא + 20.5"
        val shift = SmartParser.parseLine(line, 45.0)
        assertNotNull(shift)

        val calStart = Calendar.getInstance().apply { timeInMillis = shift!!.startTime }
        val calEnd = Calendar.getInstance().apply { timeInMillis = shift!!.endTime!! }

        assertEquals(2026, calStart.get(Calendar.YEAR))
        assertEquals(7, calStart.get(Calendar.DAY_OF_MONTH))
        assertEquals(22, calStart.get(Calendar.HOUR_OF_DAY))

        // Ends next day (Sept 8)
        assertEquals(8, calEnd.get(Calendar.DAY_OF_MONTH))
        assertEquals(6, calEnd.get(Calendar.HOUR_OF_DAY))

        assertEquals(0, shift!!.breakDurationMinutes)
        assertEquals(20.5, shift.tips, 0.001)

        // 8 hours total
        assertEquals(8.0, shift.netDurationHours, 0.05)
    }

    @Test
    fun testShiftCalculations() {
        val shift = Shift(
            startTime = 1000L,
            endTime = 1000L + (5 * 3600 * 1000L), // 5 hours
            breakDurationMinutes = 30,
            isBreakPaid = false,
            hourlyRate = 50.0,
            tips = 40.0,
            expenses = listOf(Expense(title = "Travel", amount = 15.0))
        )

        // Net hours: 5h - 30m = 4.5h
        assertEquals(4.5, shift.netDurationHours, 0.01)
        // Gross: 4.5 * 50 = 225
        assertEquals(225.0, shift.grossEarnings, 0.01)
        // Net: 225 + 40 - 15 = 250
        assertEquals(250.0, shift.netPay, 0.01)
    }
}


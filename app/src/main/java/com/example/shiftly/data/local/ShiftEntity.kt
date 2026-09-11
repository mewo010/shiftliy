package com.example.shiftly.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.shiftly.data.model.Expense
import com.example.shiftly.data.model.Shift
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey val id: String,
    val startTime: Long,
    val endTime: Long?,
    val breakDurationMinutes: Int,
    val isBreakPaid: Boolean,
    val expensesJson: String,
    val tips: Double,
    val jobRole: String,
    val notes: String,
    val hourlyRate: Double
) {
    fun toDomain(): Shift {
        val expenses = ExpenseConverters.jsonToExpenses(expensesJson)
        return Shift(
            id = id,
            startTime = startTime,
            endTime = endTime,
            breakDurationMinutes = breakDurationMinutes,
            isBreakPaid = isBreakPaid,
            expenses = expenses,
            tips = tips,
            jobRole = jobRole,
            notes = notes,
            hourlyRate = hourlyRate
        )
    }

    companion object {
        fun fromDomain(shift: Shift): ShiftEntity {
            return ShiftEntity(
                id = shift.id,
                startTime = shift.startTime,
                endTime = shift.endTime,
                breakDurationMinutes = shift.breakDurationMinutes,
                isBreakPaid = shift.isBreakPaid,
                expensesJson = ExpenseConverters.expensesToJson(shift.expenses),
                tips = shift.tips,
                jobRole = shift.jobRole,
                notes = shift.notes,
                hourlyRate = shift.hourlyRate
            )
        }
    }
}

object ExpenseConverters {
    @TypeConverter
    fun expensesToJson(expenses: List<Expense>): String {
        val array = JSONArray()
        for (exp in expenses) {
            val obj = JSONObject().apply {
                put("id", exp.id)
                put("title", exp.title)
                put("amount", exp.amount)
                put("category", exp.category)
            }
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun jsonToExpenses(json: String?): List<Expense> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<Expense>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Expense(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        amount = obj.optDouble("amount", 0.0),
                        category = obj.optString("category", "כללי")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}

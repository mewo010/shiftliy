package com.example.shiftly.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shiftly.data.model.Shift
import com.example.ui.theme.ShiftlyCoral
import com.example.ui.theme.ShiftlyGreen
import com.example.ui.theme.ShiftlyShabbatBg
import com.example.ui.theme.ShiftlyShabbatPurple
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    shifts: List<Shift>,
    onShiftClick: (Shift) -> Unit,
    onDeleteShift: (String) -> Unit,
    onAddManualShift: (Long) -> Unit
) {
    var selectedCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
        })
    }

    var selectedDay by remember {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        mutableIntStateOf(today)
    }

    val currentYear = selectedCalendar.get(Calendar.YEAR)
    val currentMonth = selectedCalendar.get(Calendar.MONTH)

    val hebrewMonthNames = listOf(
        "ינואר", "פברואר", "מרץ", "אפריל", "מאי", "יוני",
        "יולי", "אוגוסט", "ספטמבר", "אוקטובר", "נובמבר", "דצמבר"
    )

    val dayHeaders = listOf("א׳", "ב׳", "ג׳", "ד׳", "ה׳", "ו׳", "שבת")

    // Determine days in month and starting day of week (Sunday=1)
    val maxDays = selectedCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = selectedCalendar.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 7 = Saturday

    // Shifts grouped by day of this month
    val shiftsByDay = remember(shifts, currentYear, currentMonth) {
        val map = mutableMapOf<Int, MutableList<Shift>>()
        val cal = Calendar.getInstance()
        for (s in shifts) {
            cal.timeInMillis = s.startTime
            if (cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth) {
                val d = cal.get(Calendar.DAY_OF_MONTH)
                map.getOrPut(d) { mutableListOf() }.add(s)
            }
        }
        map
    }

    val selectedDayShifts = shiftsByDay[selectedDay] ?: emptyList()

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Month Navigation Header
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val newCal = (selectedCalendar.clone() as Calendar).apply {
                            add(Calendar.MONTH, -1)
                        }
                        selectedCalendar = newCal
                        selectedDay = 1
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חודש קודם")
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${hebrewMonthNames[currentMonth]} $currentYear",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${shiftsByDay.values.flatten().size} משמרות בחודש זה",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        val newCal = (selectedCalendar.clone() as Calendar).apply {
                            add(Calendar.MONTH, 1)
                        }
                        selectedCalendar = newCal
                        selectedDay = 1
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "חודש הבא")
                }
            }
        }

        // Calendar Grid
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Day of week headers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    dayHeaders.forEachIndexed { index, name ->
                        val isShabbat = index == 6 // Saturday in Israel
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isShabbat) FontWeight.Bold else FontWeight.Medium,
                            color = if (isShabbat) ShiftlyShabbatPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Days Grid (Sunday-based, up to 6 rows)
                val totalSlots = ((maxDays + (firstDayOfWeek - 1) + 6) / 7) * 7
                for (row in 0 until (totalSlots / 7)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (col in 0 until 7) {
                            val slotIndex = row * 7 + col
                            val dayNumber = slotIndex - (firstDayOfWeek - 1) + 1
                            val isValidDay = dayNumber in 1..maxDays
                            val isShabbat = col == 6 // Saturday
                            val isSelected = isValidDay && dayNumber == selectedDay
                            val hasShifts = isValidDay && shiftsByDay.containsKey(dayNumber)
                            val shiftCount = shiftsByDay[dayNumber]?.size ?: 0

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            !isValidDay -> Color.Transparent
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isShabbat -> ShiftlyShabbatBg.copy(alpha = 0.7f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable(enabled = isValidDay) {
                                        if (isValidDay) selectedDay = dayNumber
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isValidDay) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = dayNumber.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected || isShabbat) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                                isShabbat -> ShiftlyShabbatPurple
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )

                                        // Shift indicator dot or badge
                                        if (hasShifts) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                        else MaterialTheme.colorScheme.primary
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Day Details Header & Add Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "משמרות ליום $selectedDay ב${hebrewMonthNames[currentMonth]} (${selectedDayShifts.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            TextButton(
                onClick = {
                    val cal = Calendar.getInstance().apply {
                        set(currentYear, currentMonth, selectedDay, 9, 0, 0)
                    }
                    onAddManualShift(cal.timeInMillis)
                },
                modifier = Modifier.testTag("add_manual_shift_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("הוסף משמרת")
            }
        }

        // Shifts for Selected Day
        if (selectedDayShifts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.EventBusy,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "אין משמרות רשומות ביום זה",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(selectedDayShifts, key = { it.id }) { shift ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                                onDeleteShift(shift.id)
                                true
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val color by animateColorAsState(
                                if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) ShiftlyCoral else Color.Transparent,
                                label = "dismiss_color"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "מחק משמרת",
                                    tint = Color.White
                                )
                            }
                        }
                    ) {
                        Card(
                            onClick = { onShiftClick(shift) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth().testTag("shift_card_${shift.id}")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = shift.jobRole,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${timeFormat.format(Date(shift.startTime))} - ${timeFormat.format(Date(shift.endTime ?: shift.startTime))}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Text(
                                        text = "₪${String.format(Locale.US, "%.1f", shift.netPay)}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${String.format(Locale.US, "%.2f", shift.netDurationHours)} שעות עבודה נטו",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (shift.tips > 0) {
                                            Text(
                                                text = "+₪${String.format(Locale.US, "%.0f", shift.tips)} טיפ",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ShiftlyGreen,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (shift.totalExpenses > 0) {
                                            Text(
                                                text = "-₪${String.format(Locale.US, "%.0f", shift.totalExpenses)} הוצאה",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ShiftlyCoral,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (shift.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = shift.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

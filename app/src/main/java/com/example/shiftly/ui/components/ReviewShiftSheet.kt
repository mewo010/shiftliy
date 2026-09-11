package com.example.shiftly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shiftly.data.model.Expense
import com.example.shiftly.data.model.Shift
import com.example.ui.theme.ShiftlyCoral
import com.example.ui.theme.ShiftlyGreen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewShiftSheet(
    shift: Shift,
    availableRoles: List<String>,
    onDismiss: () -> Unit,
    onSave: (Shift) -> Unit,
    onDiscard: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    var jobRole by remember { mutableStateOf(shift.jobRole) }
    var breakMinutesText by remember { mutableStateOf(shift.breakDurationMinutes.toString()) }
    var isBreakPaid by remember { mutableStateOf(shift.isBreakPaid) }
    var tipsText by remember { mutableStateOf(if (shift.tips > 0) shift.tips.toString() else "") }
    var hourlyRateText by remember { mutableStateOf(shift.hourlyRate.toString()) }
    var notes by remember { mutableStateOf(shift.notes) }

    var expenses by remember { mutableStateOf(shift.expenses) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }

    // Start / End adjustments
    var startTimeMs by remember { mutableLongStateOf(shift.startTime) }
    var endTimeMs by remember { mutableLongStateOf(shift.endTime ?: System.currentTimeMillis()) }

    val breakMinutes = breakMinutesText.toIntOrNull() ?: 0
    val tips = tipsText.toDoubleOrNull() ?: 0.0
    val hourlyRate = hourlyRateText.toDoubleOrNull() ?: shift.hourlyRate

    val previewShift = remember(startTimeMs, endTimeMs, breakMinutes, isBreakPaid, tips, hourlyRate, expenses, jobRole, notes) {
        Shift(
            id = shift.id,
            startTime = startTimeMs,
            endTime = endTimeMs,
            breakDurationMinutes = breakMinutes,
            isBreakPaid = isBreakPaid,
            expenses = expenses,
            tips = tips,
            jobRole = jobRole,
            notes = notes,
            hourlyRate = hourlyRate
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Title & Date Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "סיכום ועריכת משמרת",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateFormat.format(Date(startTimeMs)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDiscard,
                    modifier = Modifier.testTag("discard_shift_button")
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "מחק משמרת",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Time range card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "זמני משמרת",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Start Time
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "כניסה",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = timeFormat.format(Date(startTimeMs)),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row {
                                        IconButton(
                                            onClick = { startTimeMs -= 15 * 60 * 1000 },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "מינוס 15 דק", modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = { startTimeMs += 15 * 60 * 1000 },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "פלוס 15 דק", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                // End Time
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "יציאה",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = timeFormat.format(Date(endTimeMs)),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row {
                                        IconButton(
                                            onClick = { endTimeMs -= 15 * 60 * 1000 },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "מינוס 15 דק", modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = { endTimeMs += 15 * 60 * 1000 },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "פלוס 15 דק", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Job role selector
                item {
                    Text(
                        text = "תפקיד / מקום עבודה",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableRoles.take(4).forEach { role ->
                            val selected = role == jobRole
                            FilterChip(
                                selected = selected,
                                onClick = { jobRole = role },
                                label = { Text(role) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }

                // Break section & Paid toggle
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "הפסקה בתשלום",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isBreakPaid) "ההפסקה נחשבת בשכר" else "ההפסקה מקוזזת משעות העבודה",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isBreakPaid,
                                    onCheckedChange = { isBreakPaid = it },
                                    modifier = Modifier.testTag("break_paid_switch")
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = breakMinutesText,
                                onValueChange = { breakMinutesText = it },
                                label = { Text("משך הפסקה (בדקות)") },
                                leadingIcon = { Icon(Icons.Default.Coffee, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("break_minutes_input")
                            )
                        }
                    }
                }

                // Rate & Tips
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = hourlyRateText,
                            onValueChange = { hourlyRateText = it },
                            label = { Text("תעריף שעתי (₪)") },
                            leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("hourly_rate_input")
                        )

                        OutlinedTextField(
                            value = tipsText,
                            onValueChange = { tipsText = it },
                            label = { Text("טיפים (₪)") },
                            leadingIcon = { Icon(Icons.Default.MonetizationOn, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("tips_input")
                        )
                    }
                }

                // Expenses Section
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "הוצאות משמרת (נסיעות, אוכל וכו')",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                TextButton(
                                    onClick = { showAddExpenseDialog = true },
                                    modifier = Modifier.testTag("add_expense_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("הוסף הוצאה")
                                }
                            }

                            if (expenses.isEmpty()) {
                                Text(
                                    text = "אין הוצאות רשומות למשמרת זו",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                expenses.forEach { exp ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(text = exp.title, fontWeight = FontWeight.Medium)
                                            Text(
                                                text = exp.category,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "-₪${String.format(Locale.US, "%.1f", exp.amount)}",
                                                color = ShiftlyCoral,
                                                fontWeight = FontWeight.Bold
                                            )
                                            IconButton(
                                                onClick = { expenses = expenses.filter { it.id != exp.id } },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "מחק", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Notes
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("הערות") },
                        leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("notes_input")
                    )
                }

                // Live Summary Calculation Box
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "סיכום שכר משוער",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("שעות נטו לתשלום:")
                                Text(
                                    "${String.format(Locale.US, "%.2f", previewShift.netDurationHours)} שעות",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("שכר בסיס (ברוטו):")
                                Text(
                                    "₪${String.format(Locale.US, "%.1f", previewShift.grossEarnings)}",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (previewShift.tips > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("טיפים:")
                                    Text(
                                        "+₪${String.format(Locale.US, "%.1f", previewShift.tips)}",
                                        fontWeight = FontWeight.Bold,
                                        color = ShiftlyGreen
                                    )
                                }
                            }
                            if (previewShift.totalExpenses > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("הוצאות שקוזזו:")
                                    Text(
                                        "-₪${String.format(Locale.US, "%.1f", previewShift.totalExpenses)}",
                                        fontWeight = FontWeight.Bold,
                                        color = ShiftlyCoral
                                    )
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "סה״כ לתשלום נטו:",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "₪${String.format(Locale.US, "%.1f", previewShift.netPay)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: Save or Cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).testTag("cancel_review_button")
                ) {
                    Text("ביטול")
                }

                Button(
                    onClick = { onSave(previewShift) },
                    modifier = Modifier.weight(1.5f).testTag("save_shift_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("שמור משמרת")
                }
            }
        }
    }

    // Add Expense Dialog
    if (showAddExpenseDialog) {
        var expTitle by remember { mutableStateOf("") }
        var expAmountText by remember { mutableStateOf("") }
        var expCategory by remember { mutableStateOf("נסיעות") }
        val categories = listOf("נסיעות", "אוכל", "ציוד", "אחר")

        AlertDialog(
            onDismissRequest = { showAddExpenseDialog = false },
            title = { Text("הוספת הוצאה למשמרת") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = expTitle,
                        onValueChange = { expTitle = it },
                        label = { Text("תיאור (למשל: מונית חזור)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = expAmountText,
                        onValueChange = { expAmountText = it },
                        label = { Text("סכום ב-₪") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("קטגוריה:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = cat == expCategory,
                                onClick = { expCategory = cat },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = expAmountText.toDoubleOrNull() ?: 0.0
                        if (expTitle.isNotBlank() && amount > 0) {
                            expenses = expenses + Expense(
                                title = expTitle,
                                amount = amount,
                                category = expCategory
                            )
                            showAddExpenseDialog = false
                        }
                    }
                ) {
                    Text("הוסף")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExpenseDialog = false }) {
                    Text("ביטול")
                }
            }
        )
    }
}

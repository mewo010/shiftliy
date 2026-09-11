package com.example.shiftly.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shiftly.data.model.Shift
import com.example.ui.theme.ShiftlyAmber
import com.example.ui.theme.ShiftlyAmberContainer
import com.example.ui.theme.ShiftlyGreen
import com.example.ui.theme.ShiftlyGreenContainer
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TimerScreen(
    activeShift: Shift?,
    isBreakActive: Boolean,
    elapsedSeconds: Long,
    breakSeconds: Long,
    hourlyRate: Double,
    availableRoles: List<String>,
    onStartShift: (String, Boolean) -> Unit,
    onToggleBreakType: (Boolean) -> Unit,
    onPauseForBreak: () -> Unit,
    onResumeFromBreak: () -> Unit,
    onStopShift: () -> Unit,
    onDiscardShift: () -> Unit,
    onOpenRateSettings: () -> Unit
) {
    var selectedRole by remember { mutableStateOf("מלצרות") }
    var isBreakPaidPreference by remember { mutableStateOf(false) }

    val isRunning = activeShift != null

    // Format timer
    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60
    val timerText = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)

    // Break timer format
    val bMinutes = breakSeconds / 60
    val bSeconds = breakSeconds % 60
    val breakTimerText = String.format(Locale.US, "%02d:%02d", bMinutes, bSeconds)

    // Live calculation
    val unpaidBreakSec = if (activeShift?.isBreakPaid == true) 0L else breakSeconds
    val netWorkSeconds = (elapsedSeconds - unpaidBreakSec).coerceAtLeast(0L)
    val liveNetHours = netWorkSeconds / 3600.0
    val liveEstimatedPay = liveNetHours * hourlyRate

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status & Rate Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Chip
            val statusColor by animateColorAsState(
                targetValue = when {
                    !isRunning -> MaterialTheme.colorScheme.surfaceVariant
                    isBreakActive -> ShiftlyAmberContainer
                    else -> ShiftlyGreenContainer
                },
                label = "status_color"
            )
            val statusTextColor = when {
                !isRunning -> MaterialTheme.colorScheme.onSurfaceVariant
                isBreakActive -> Color(0xFFB45309)
                else -> Color(0xFF047857)
            }

            Surface(
                color = statusColor,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusTextColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            !isRunning -> "לא פעיל"
                            isBreakActive -> "בהפסקה (${breakTimerText})"
                            else -> "משמרת פעילה"
                        },
                        color = statusTextColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Hourly Rate Pill
            Surface(
                onClick = onOpenRateSettings,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("rate_settings_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₪${String.format(Locale.US, "%.0f", hourlyRate)}/שעה",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "שנה תעריף",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Main Timer Hero Display
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isRunning && activeShift != null) {
                    Text(
                        text = "תפקיד: ${activeShift.jobRole}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "החלה בשעה: ${timeFormat.format(Date(activeShift.startTime))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    Text(
                        text = "שעון נוכחות וטיימר משמרת",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Digital Timer Typography
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = timerText,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 48.sp,
                                letterSpacing = 2.sp
                            ),
                            color = if (isBreakActive) ShiftlyAmber else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Break Type Toggle Switch
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val paid = if (isRunning) activeShift?.isBreakPaid == true else isBreakPaidPreference
                            Text(
                                text = if (paid) "הפסקה בתשלום" else "הפסקה ללא תשלום",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (paid) "זמן ההפסקה נספר בשכר השעתי" else "זמן ההפסקה מקוזז מהשכר",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = if (isRunning) activeShift?.isBreakPaid == true else isBreakPaidPreference,
                            onCheckedChange = { checked ->
                                if (isRunning) {
                                    onToggleBreakType(checked)
                                } else {
                                    isBreakPaidPreference = checked
                                }
                            },
                            modifier = Modifier.testTag("timer_break_paid_toggle")
                        )
                    }
                }
            }
        }

        // Live Calculation Box
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "שכר משוער למשמרת זו",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "לפי ₪${String.format(Locale.US, "%.0f", hourlyRate)}/שעה",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "₪${String.format(Locale.US, "%.2f", liveEstimatedPay)}",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "שכר נטו משוער (ללא טיפים)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${String.format(Locale.US, "%.2f", liveNetHours)} שעות",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "שעות עבודה נטו",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                if (breakSeconds > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "זמן הפסקה: ${bMinutes} דקות (${if (activeShift?.isBreakPaid == true) "משולמת" else "ללא תשלום"})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // If not running: Select Job Role before starting
        AnimatedVisibility(visible = !isRunning) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "בחר תפקיד למשמרת הקרובה:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableRoles.take(3).forEach { role ->
                            val isSelected = role == selectedRole
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedRole = role },
                                label = { Text(role) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }
        }

        // Accessible Material 3 Action Buttons
        if (!isRunning) {
            // Big Start Button
            Button(
                onClick = { onStartShift(selectedRole, isBreakPaidPreference) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .testTag("start_shift_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ShiftlyGreen
                )
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "התחל משמרת",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            // Running State Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Break / Resume Button
                    if (isBreakActive) {
                        Button(
                            onClick = onResumeFromBreak,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("resume_shift_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ShiftlyGreen)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("חזור לעבודה", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        FilledTonalButton(
                            onClick = onPauseForBreak,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("take_break_button"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Coffee, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("צא להפסקה", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Stop & Review Button
                    Button(
                        onClick = onStopShift,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(56.dp)
                            .testTag("stop_and_review_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("סיום ועריכה", fontWeight = FontWeight.Bold)
                    }
                }

                // Discard Button
                TextButton(
                    onClick = onDiscardShift,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("discard_active_button")
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "בטל משמרת נוכחית ללא שמירה",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

package com.example.shiftly.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shiftly.data.model.Shift
import com.example.shiftly.ui.components.RateSettingsDialog
import com.example.shiftly.ui.components.ReviewShiftSheet
import com.example.shiftly.ui.components.SmartPasteDialog
import com.example.shiftly.ui.screens.AnalyticsScreen
import com.example.shiftly.ui.screens.CalendarScreen
import com.example.shiftly.ui.screens.TimerScreen
import com.example.shiftly.viewmodel.ShiftViewModel
import kotlinx.coroutines.launch
import java.util.UUID

enum class ShiftlyTab(val title: String) {
    TIMER("טיימר"),
    CALENDAR("יומן והיסטוריה"),
    ANALYTICS("אנליטיקה")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftlyApp(viewModel: ShiftViewModel) {
    // Strictly Hebrew RTL directionality
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val shifts by viewModel.shifts.collectAsStateWithLifecycle()
        val activeShift by viewModel.activeShift.collectAsStateWithLifecycle()
        val isBreakActive by viewModel.isBreakActive.collectAsStateWithLifecycle()
        val elapsedSeconds by viewModel.currentElapsedSeconds.collectAsStateWithLifecycle()
        val breakSeconds by viewModel.currentBreakSeconds.collectAsStateWithLifecycle()
        val hourlyRate by viewModel.hourlyRate.collectAsStateWithLifecycle()
        val reviewingShift by viewModel.reviewingShift.collectAsStateWithLifecycle()

        var currentTab by remember { mutableStateOf(ShiftlyTab.TIMER) }
        var showSmartPasteDialog by remember { mutableStateOf(false) }
        var showRateSettingsDialog by remember { mutableStateOf(false) }

        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Shiftly",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        // Smart Paste Button
                        FilledTonalButton(
                            onClick = { showSmartPasteDialog = true },
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .testTag("open_smart_paste_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentPaste,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("הדבקה מ-WhatsApp", style = MaterialTheme.typography.labelMedium)
                        }

                        // Hourly Rate Settings Button
                        IconButton(
                            onClick = { showRateSettingsDialog = true },
                            modifier = Modifier.testTag("rate_settings_icon_button")
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "הגדרות תעריף",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        selected = currentTab == ShiftlyTab.TIMER,
                        onClick = { currentTab = ShiftlyTab.TIMER },
                        icon = {
                            Icon(
                                if (currentTab == ShiftlyTab.TIMER) Icons.Default.Timer else Icons.Default.Timer,
                                contentDescription = null
                            )
                        },
                        label = { Text(ShiftlyTab.TIMER.title) },
                        modifier = Modifier.testTag("nav_timer_tab")
                    )

                    NavigationBarItem(
                        selected = currentTab == ShiftlyTab.CALENDAR,
                        onClick = { currentTab = ShiftlyTab.CALENDAR },
                        icon = {
                            Icon(
                                if (currentTab == ShiftlyTab.CALENDAR) Icons.Default.CalendarMonth else Icons.Default.CalendarMonth,
                                contentDescription = null
                            )
                        },
                        label = { Text(ShiftlyTab.CALENDAR.title) },
                        modifier = Modifier.testTag("nav_calendar_tab")
                    )

                    NavigationBarItem(
                        selected = currentTab == ShiftlyTab.ANALYTICS,
                        onClick = { currentTab = ShiftlyTab.ANALYTICS },
                        icon = {
                            Icon(
                                if (currentTab == ShiftlyTab.ANALYTICS) Icons.Default.BarChart else Icons.Default.BarChart,
                                contentDescription = null
                            )
                        },
                        label = { Text(ShiftlyTab.ANALYTICS.title) },
                        modifier = Modifier.testTag("nav_analytics_tab")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Crossfade(targetState = currentTab, label = "tab_crossfade") { tab ->
                    when (tab) {
                        ShiftlyTab.TIMER -> {
                            TimerScreen(
                                activeShift = activeShift,
                                isBreakActive = isBreakActive,
                                elapsedSeconds = elapsedSeconds,
                                breakSeconds = breakSeconds,
                                hourlyRate = hourlyRate,
                                availableRoles = viewModel.availableJobRoles,
                                onStartShift = { role, isPaid ->
                                    viewModel.startShift(role, isPaid)
                                },
                                onToggleBreakType = { isPaid ->
                                    viewModel.toggleBreakType(isPaid)
                                },
                                onPauseForBreak = {
                                    viewModel.pauseForBreak()
                                },
                                onResumeFromBreak = {
                                    viewModel.resumeFromBreak()
                                },
                                onStopShift = {
                                    viewModel.stopShiftToReview()
                                },
                                onDiscardShift = {
                                    viewModel.discardActiveShift()
                                    scope.launch {
                                        snackbarHostState.showSnackbar("המשמרת בוטלה")
                                    }
                                },
                                onOpenRateSettings = {
                                    showRateSettingsDialog = true
                                }
                            )
                        }

                        ShiftlyTab.CALENDAR -> {
                            CalendarScreen(
                                shifts = shifts,
                                onShiftClick = { shift ->
                                    viewModel.openShiftForReview(shift)
                                },
                                onDeleteShift = { id ->
                                    viewModel.deleteShift(id)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("המשמרת נמחקה בהצלחה")
                                    }
                                },
                                onAddManualShift = { initialStartTime ->
                                    val newShift = Shift(
                                        id = UUID.randomUUID().toString(),
                                        startTime = initialStartTime,
                                        endTime = initialStartTime + (8 * 3600 * 1000L),
                                        breakDurationMinutes = 30,
                                        isBreakPaid = false,
                                        jobRole = "מלצרות",
                                        hourlyRate = hourlyRate
                                    )
                                    viewModel.openShiftForReview(newShift)
                                }
                            )
                        }

                        ShiftlyTab.ANALYTICS -> {
                            AnalyticsScreen(shifts = shifts)
                        }
                    }
                }
            }
        }

        // Review Mode Sheet
        if (reviewingShift != null) {
            ReviewShiftSheet(
                shift = reviewingShift!!,
                availableRoles = viewModel.availableJobRoles,
                onDismiss = { viewModel.dismissReviewSheet() },
                onSave = { updatedShift ->
                    viewModel.saveShift(updatedShift)
                    scope.launch {
                        snackbarHostState.showSnackbar("המשמרת נשמרה בהצלחה")
                    }
                },
                onDiscard = {
                    val id = reviewingShift!!.id
                    viewModel.deleteShift(id)
                    viewModel.dismissReviewSheet()
                    scope.launch {
                        snackbarHostState.showSnackbar("המשמרת נמחקה")
                    }
                }
            )
        }

        // Smart Paste Dialog
        if (showSmartPasteDialog) {
            SmartPasteDialog(
                defaultHourlyRate = hourlyRate,
                onDismiss = { showSmartPasteDialog = false },
                onConfirmBatch = { importedShifts ->
                    viewModel.addParsedShifts(importedShifts)
                    showSmartPasteDialog = false
                    scope.launch {
                        snackbarHostState.showSnackbar("${importedShifts.size} משמרות יובאו בהצלחה!")
                    }
                }
            )
        }

        // Rate Settings Dialog
        if (showRateSettingsDialog) {
            RateSettingsDialog(
                currentRate = hourlyRate,
                onDismiss = { showRateSettingsDialog = false },
                onConfirm = { newRate ->
                    viewModel.setHourlyRate(newRate)
                    showRateSettingsDialog = false
                    scope.launch {
                        snackbarHostState.showSnackbar("תעריף עודכן ל-₪${newRate} לשעה")
                    }
                }
            )
        }
    }
}

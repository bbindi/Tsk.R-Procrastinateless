package com.brixavier.tskr.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.brixavier.tskr.bloc.ReminderEvent
import com.brixavier.tskr.bloc.ReminderState
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.ui.theme.*
import com.brixavier.tskr.engine.PersonalityEngine
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Screen destinations for dynamic navigation.
 */
enum class Screen {
    Home,
    Notes,
    Calendar,
    Settings
}

/**
 * Helper to calculate relative time human-readable string.
 */
fun getRelativeTimeSpan(dateStr: String, timeStr: String): String {
    try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val targetDate = sdf.parse("$dateStr $timeStr") ?: return "$dateStr $timeStr"
        val now = Date()
        val diffMillis = targetDate.time - now.time
        val diffMinutes = diffMillis / (1000 * 60)
        val diffHours = diffMinutes / 60
        val diffDays = diffHours / 24

        return when {
            diffMinutes in 0..59 -> "Starts in $diffMinutes minutes"
            diffMinutes in -59..-1 -> "Overdue by ${-diffMinutes} minutes"
            diffHours in 1..23 -> "In $diffHours hours"
            diffHours in -23..-1 -> "Overdue by ${-diffHours} hours"
            diffDays == 1L -> "Tomorrow"
            diffDays == -1L -> "Yesterday"
            diffDays > 1 -> "In $diffDays days"
            diffDays < -1 -> "Overdue by ${-diffDays} days"
            else -> "Today at $timeStr"
        }
    } catch (ignored: Exception) {
        return "$dateStr $timeStr"
    }
}

/**
 * Dynamic layout for the Notes Page displaying active reminder notes and checklists.
 */
@Composable
fun NotesPage(
    state: ReminderState,
    onEvent: (ReminderEvent) -> Unit,
    onReminderClick: (Reminder) -> Unit
) {
    val activeReminders = state.reminders.filter { it.isActive }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Tsk.R Notes",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
        Text(
            text = "Keep track of checklists and notes for active reminders.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (activeReminders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No active reminders",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                items(activeReminders) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onClick = { onReminderClick(reminder) },
                        onToggle = { isActive ->
                            onEvent(ReminderEvent.ToggleReminder(reminder.id, isActive))
                        },
                        onDelete = {
                            onEvent(ReminderEvent.DeleteReminder(reminder.id))
                        }
                    )
                }
            }
        }
    }
}

data class CalendarDay(
    val formatted: String,
    val dayName: String,
    val dayNum: Int
)

/**
 * Dynamic layout for the Calendar Page displaying a premium daily planner and schedule.
 */
@Composable
fun CalendarPage(
    state: ReminderState,
    onEvent: (ReminderEvent) -> Unit
) {
    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today.toString()) }
    
    val days = remember {
        val formatter = DateTimeFormatter.ofPattern("EEE", Locale.US)
        (0 until 14).map { i ->
            val date = today.plusDays(i.toLong())
            CalendarDay(
                formatted = date.toString(),
                dayName = date.format(formatter),
                dayNum = date.dayOfMonth
            )
        }
    }

    val filteredTasks = state.reminders.filter { it.date == selectedDate }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Tsk.R Calendar",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            Text(
                text = "Visualize your schedule and upcoming events.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        // Horizontal Week Strip using a LazyRow for smooth horizontal scrolling
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(days) { day ->
                val isSelected = selectedDate == day.formatted
                val hasTask = state.reminders.any { it.date == day.formatted }
                
                Card(
                    modifier = Modifier
                        .width(56.dp)
                        .height(80.dp)
                        .clickable { selectedDate = day.formatted },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = day.dayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                }
                            )
                        )
                        Text(
                            text = day.dayNum.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    color = if (hasTask) {
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    } else {
                                        Color.Transparent
                                    },
                                    shape = RoundedCornerShape(3.dp)
                                )
                        )
                    }
                }
            }
        }

        // Selected Date Header
        Text(
            text = selectedDate,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(top = 8.dp)
        )

        // Filtered Task List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredTasks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "No tasks",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Nothing scheduled. Enjoy your procrastination... while it lasts!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            } else {
                items(filteredTasks) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onClick = { /* Edit action simulated in BottomSheet */ },
                        onToggle = { isActive ->
                            onEvent(ReminderEvent.ToggleReminder(reminder.id, isActive))
                        },
                        onDelete = {
                            onEvent(ReminderEvent.DeleteReminder(reminder.id))
                        }
                    )
                }
            }
        }
    }
}


/**
 * Main Composable Screen for Reminders.
 *
 * Fully overhauled using Material 3 Expressive Design principles.
 *
 * @param state The current state emitted by the [ReminderBloc].
 * @param onEvent Callback to dispatch events back to the [ReminderBloc].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(
    state: ReminderState?,
    initialShowBottomSheet: Boolean = false,
    isExactAlarmAllowed: Boolean = true,
    isBatteryOptimizationIgnored: Boolean = true,
    appVersion: String = "1.0.0",
    onOpenAlarmSettings: () -> Unit = {},
    onOpenBatterySettings: () -> Unit = {},
    onEvent: (ReminderEvent) -> Unit,
    onReminderClick: (Reminder) -> Unit = {},
    onSheetClosed: () -> Unit = {}
) {
    if (state == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var showBottomSheet by remember { mutableStateOf(initialShowBottomSheet) }
    var editingReminder by remember { mutableStateOf<Reminder?>(null) }
    var isHistoryExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    var currentScreen by remember { mutableStateOf(Screen.Home) }
    
    val homeLazyListState = rememberLazyListState()
    val isFabExpanded by remember {
        derivedStateOf {
            homeLazyListState.firstVisibleItemIndex == 0
        }
    }

    // Dynamically calculate greeting based on local clock
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val (greeting, icon) = when (hour) {
        in 0..11 -> "Good morning" to "☀️"
        in 12..16 -> "Good afternoon" to "🌤️"
        else -> "Good evening" to "🌙"
    }

    // Split reminders into active list and completed history
    val activeReminders = state.reminders.filter { it.isActive }.sortedBy { "${it.date} ${it.time}" }
    val inactiveReminders = state.reminders.filter { !it.isActive }
    
    val totalTasks = state.reminders.size
    val completedTasks = inactiveReminders.size
    val progress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
    
    val nextUp = activeReminders.firstOrNull()
    val randomQuote = remember { PersonalityEngine.getRandomQuote() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentScreen) {
                Screen.Home -> {
                    LazyColumn(
                        state = homeLazyListState,
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 24.dp,
                            bottom = 120.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .animateContentSize()
                    ) {
                        // 0. App Branding Header with Settings Access
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                Text(
                                    text = "Tsk.R: Procrastinateless",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        letterSpacing = 1.sp
                                    ),
                                    modifier = Modifier.align(Alignment.Center)
                                )

                                IconButton(
                                    onClick = { currentScreen = Screen.Settings },
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        // 1. Time-Based Greeting Dashboard Header
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$greeting $icon",
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    ),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = randomQuote,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))

                                // Streak Badge
                                if (state.streak > 0) {
                                    val isCrown = state.streak >= 5
                                    Surface(
                                        color = if (isCrown) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.shadow(if (isCrown) 8.dp else 2.dp, RoundedCornerShape(16.dp))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(if (isCrown) "👑" else "🔥", fontSize = 18.sp)
                                            Text(
                                                text = "${state.streak} DAY",
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCrown) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }

                        // 2. Visual Progress Dashboard Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(28.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Daily Progress",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            )
                                        )
                                        Text(
                                            text = "${(progress * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = if (activeReminders.isNotEmpty()) {
                                            "Keep going! $totalTasks tasks in total today."
                                        } else if (totalTasks > 0) {
                                            "🎉 You're all caught up. Go celebrate with a snack."
                                        } else {
                                            "Ready to start your mission?"
                                        },
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                        }
                        
                        // 2.5 Next Up Focus Card
                        if (nextUp != null) {
                            item {
                                Text(
                                    text = "Coming Up Next",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(20.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Timer,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = nextUp.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = getRelativeTimeSpan(nextUp.date, nextUp.time),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Active Reminders Section
                        if (activeReminders.isNotEmpty()) {
                            item {
                                Text(
                                    text = "All Reminders",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                    ),
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }

                            items(
                                items = activeReminders,
                                key = { it.id }
                            ) { reminder ->
                                ReminderCard(
                                    reminder = reminder,
                                    onClick = {
                                        editingReminder = reminder
                                        showBottomSheet = true
                                    },
                                    onToggle = { isActive ->
                                        onEvent(ReminderEvent.ToggleReminder(reminder.id, isActive))
                                    },
                                    onDelete = {
                                        onEvent(ReminderEvent.DeleteReminder(reminder.id))
                                    }
                                )
                            }
                        } else if (inactiveReminders.isEmpty()) {
                            // Empty state view - brand new user
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No reminders yet 😴",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Tap the '+' button to schedule your first mission.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        } else {
                            // Mission Complete empty state
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Mission Complete 🏆",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Your future self is proud of you. $randomQuote",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 32.dp)
                                    )
                                }
                            }
                        }

                        // 4. Collapsible History Accordion
                        if (inactiveReminders.isNotEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isHistoryExpanded = !isHistoryExpanded },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isHistoryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = "Toggle History",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "History (${inactiveReminders.size} completed)",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            if (isHistoryExpanded) {
                                items(
                                    items = inactiveReminders,
                                    key = { it.id }
                                ) { reminder ->
                                    ReminderCard(
                                        reminder = reminder,
                                        onClick = {
                                            editingReminder = reminder
                                            showBottomSheet = true
                                        },
                                        onToggle = { isActive ->
                                            onEvent(ReminderEvent.ToggleReminder(reminder.id, isActive))
                                        },
                                        onDelete = {
                                            onEvent(ReminderEvent.DeleteReminder(reminder.id))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                Screen.Notes -> {
                    NotesPage(state = state, onEvent = onEvent, onReminderClick = onReminderClick)
                }
                Screen.Calendar -> {
                    CalendarPage(state = state, onEvent = onEvent)
                }
                Screen.Settings -> {
                    SettingsPage(
                        isExactAlarmAllowed = isExactAlarmAllowed,
                        isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
                        appVersion = appVersion,
                        onOpenAlarmSettings = onOpenAlarmSettings,
                        onOpenBatterySettings = onOpenBatterySettings
                    )
                }
            }

            // Floating Navigation & FAB Container
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.wrapContentSize()
                ) {
                    // 1. Refined Floating Navigation Pill
                    Surface(
                        modifier = Modifier
                            .height(64.dp)
                            .shadow(8.dp, CircleShape)
                            .border(
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                CircleShape
                            ),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                        tonalElevation = 12.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val navigationItems = listOf(
                                Triple(Screen.Home, Icons.Default.Home, "Home"),
                                Triple(Screen.Notes, Icons.Default.Edit, "Notes"),
                                Triple(Screen.Calendar, Icons.Default.CalendarToday, "Calendar")
                            )

                            navigationItems.forEach { (screen, icon, label) ->
                                val selected = currentScreen == screen
                                val animatedContainerColor by animateColorAsState(
                                    targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                    label = "nav_container"
                                )
                                val animatedIconColor by animateColorAsState(
                                    targetValue = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    label = "nav_icon"
                                )

                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(animatedContainerColor)
                                        .clickable { currentScreen = screen },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = animatedIconColor
                                    )
                                }
                            }
                        }
                    }

                    // 2. Reactive Spring-Animated Extended FAB
                    ExtendedFloatingActionButton(
                        onClick = {
                            editingReminder = null
                            showBottomSheet = true
                        },
                        expanded = isFabExpanded,
                        icon = { Icon(Icons.Default.Add, "Add Reminder") },
                        text = { Text("New Reminder") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.height(64.dp)
                    )
                }
            }

            // Add Reminder Modal Bottom Sheet
            if (showBottomSheet) {
                AddReminderBottomSheet(
                    reminderToEdit = editingReminder,
                    sheetState = sheetState,
                    onDismiss = { 
                        showBottomSheet = false
                        editingReminder = null
                        onSheetClosed()
                    },
                    onSave = { title, date, time, priority, notes, subTasksRaw, motivation ->
                        val editRem = editingReminder
                        if (editRem != null) {
                            onEvent(ReminderEvent.UpdateReminder(editRem.copy(
                                title = title,
                                date = date,
                                time = time,
                                priority = priority,
                                notes = notes,
                                subTasksRaw = subTasksRaw,
                                motivation = motivation
                            )))
                        } else {
                            onEvent(ReminderEvent.AddReminder(title, date, time, priority, notes, subTasksRaw, motivation))
                        }
                        showBottomSheet = false
                        editingReminder = null
                        onSheetClosed()
                    }
                )
            }
        }
    }
}

/**
 * A beautiful, minimalist Card displaying a single reminder.
 */
@Composable
fun ReminderCard(
    reminder: Reminder,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val alpha by animateFloatAsState(targetValue = if (reminder.isActive) 1f else 0.5f)
    val strikeThrough = if (reminder.isActive) TextDecoration.None else TextDecoration.LineThrough

    val priorityColor = when {
        !reminder.isActive -> SuccessGreen
        reminder.isImportant() -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    val activeGradientBrush = Brush.linearGradient(
        colors = listOf(
            priorityColor.copy(alpha = 0.15f),
            Color.Transparent
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { onClick() }
            .then(
                if (reminder.isActive) {
                    Modifier
                        .background(brush = activeGradientBrush, shape = RoundedCornerShape(16.dp))
                        .border(
                            width = 1.dp,
                            color = priorityColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.isActive) {
                Color.Transparent
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority Indicator Strip
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(priorityColor.copy(alpha = alpha))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp)
                ) {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha),
                            textDecoration = strikeThrough
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Date display with relative formatting
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Date",
                                tint = priorityColor.copy(alpha = alpha),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (reminder.isActive) getRelativeTimeSpan(reminder.date, reminder.time) else reminder.date,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                            )
                        }

                        // Time display
                        if (!reminder.isActive) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = "Time",
                                    tint = priorityColor.copy(alpha = alpha),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = reminder.time,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Delete button
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }

                    // Checkmark IconButton for active state
                    IconButton(
                        onClick = { onToggle(!reminder.isActive) },
                    ) {
                        Icon(
                            imageVector = if (!reminder.isActive) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Toggle completion",
                            tint = if (!reminder.isActive) SuccessGreen else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bottom Sheet input form to add or edit a reminder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderBottomSheet(
    reminderToEdit: Reminder? = null,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSave: (title: String, date: String, time: String, priority: String, notes: String, subTasksRaw: String, motivation: String) -> Unit
) {
    val now = remember { Calendar.getInstance() }
    val defaultDate = remember(now) { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now.time) }
    val defaultTime = remember(now) { SimpleDateFormat("HH:mm", Locale.US).format(now.time) }

    var taskName by remember { mutableStateOf(reminderToEdit?.title ?: "") }
    var selectedDate by remember { mutableStateOf(reminderToEdit?.date ?: defaultDate) }
    var selectedTime by remember { mutableStateOf(reminderToEdit?.time ?: defaultTime) }
    var priority by remember { mutableStateOf(reminderToEdit?.priority ?: Reminder.PRIORITY_NORMAL) }
    var notes by remember { mutableStateOf(reminderToEdit?.notes ?: "") }
    var subTasksRaw by remember { mutableStateOf(reminderToEdit?.subTasksRaw ?: "") }
    var motivation by remember { mutableStateOf(reminderToEdit?.motivation ?: "") }
    var showAdvanced by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        key(reminderToEdit?.id) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 36.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (reminderToEdit != null) "Edit Reminder" else "New Reminder",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
    
                // Text input for reminder title
                OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    label = { Text("What needs to be done?") },
                    placeholder = { Text("e.g. Call dentist") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
    
                // AI Task Splitting Button Removed for production stability
    
                // Helper to format date beautifully
                val formattedDateDisplay = remember(selectedDate) {
                    try {
                        val sdfSource = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        val date = sdfSource.parse(selectedDate)
                        if (date != null) {
                            val sdfDest = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US)
                            sdfDest.format(date)
                        } else {
                            selectedDate
                        }
                    } catch (e: Exception) {
                        selectedDate
                    }
                }
    
                // Helper to format time beautifully based on device settings (12h/24h)
                val formattedTimeDisplay = remember(selectedTime, context) {
                    try {
                        val parts = selectedTime.split(":")
                        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 12
                        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
                        if (is24Hour) {
                            String.format(java.util.Locale.US, "%02d:%02d", hour, minute)
                        } else {
                            val amPm = if (hour >= 12) "PM" else "AM"
                            val displayHour = when {
                                hour == 0 -> 12
                                hour > 12 -> hour - 12
                                else -> hour
                            }
                            String.format(java.util.Locale.US, "%d:%02d %s", displayHour, minute, amPm)
                        }
                    } catch (e: Exception) {
                        selectedTime
                    }
                }
    
                // Row for trigger pickers (using filled Assist Chips)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { showDatePicker = true },
                        label = { Text(text = formattedDateDisplay, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Select Date",
                                modifier = Modifier.size(AssistChipDefaults.IconSize)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        border = null,
                        shape = RoundedCornerShape(12.dp)
                    )
    
                    AssistChip(
                        onClick = { showTimePicker = true },
                        label = { Text(text = formattedTimeDisplay, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Select Time",
                                modifier = Modifier.size(AssistChipDefaults.IconSize)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        border = null,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
    
                if (showDatePicker) {
                    M3ModalDatePicker(
                        initialDate = selectedDate,
                        onDateSelected = { selectedDate = it },
                        onDismiss = { showDatePicker = false }
                    )
                }
    
                if (showTimePicker) {
                    M3TimePickerDialog(
                        initialTime = selectedTime,
                        onTimeSelected = { selectedTime = it },
                        onDismiss = { showTimePicker = false }
                    )
                }
    
                Text(
                    text = "Urgency Tier",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
    
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val isImportant = Reminder.isImportant(priority)
                    Button(
                        onClick = { priority = Reminder.PRIORITY_IMPORTANT },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isImportant) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isImportant) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🔥 Important")
                    }
    
                    val isNormal = !isImportant
                    Button(
                        onClick = { priority = Reminder.PRIORITY_NORMAL },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isNormal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isNormal) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🙂 Normal")
                    }
                }
    
                Spacer(modifier = Modifier.height(8.dp))
    
                // Advanced Settings Accordion
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    onClick = { showAdvanced = !showAdvanced }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Advanced Settings",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
    
                        AnimatedVisibility(visible = showAdvanced) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = notes,
                                    onValueChange = { notes = it },
                                    label = { Text("Notes") },
                                    placeholder = { Text("Add more details...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2
                                )
    
                                OutlinedTextField(
                                    value = subTasksRaw,
                                    onValueChange = { subTasksRaw = it },
                                    label = { Text("Checklist (pipe-separated)") },
                                    placeholder = { Text("Subtask 1|Subtask 2") },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.List, contentDescription = null) }
                                )
    
                                OutlinedTextField(
                                    value = motivation,
                                    onValueChange = { motivation = it },
                                    label = { Text("Why am I doing this?") },
                                    placeholder = { Text("Enter your motivation...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.Psychology, contentDescription = null) }
                                )
                            }
                        }
                    }
                }
    
                Spacer(modifier = Modifier.height(8.dp))
    
                // Action Buttons (Save/Cancel)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.outline)
                    }
    
                    Button(
                        onClick = {
                            if (taskName.isNotBlank()) {
                                onSave(taskName, selectedDate, selectedTime, priority, notes, subTasksRaw, motivation)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = taskName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (reminderToEdit != null) "Save Changes" else "Create",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3ModalDatePicker(
    initialDate: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            sdf.parse(initialDate)?.time
        } catch (e: Exception) {
            null
        } ?: System.currentTimeMillis()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        val formatted = sdf.format(java.util.Date(millis))
                        onDateSelected(formatted)
                    }
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3TimePickerDialog(
    initialTime: String,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val parts = initialTime.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 12
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val is24Hour = android.text.format.DateFormat.is24HourFormat(context)

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = is24Hour
    )

    var showDial by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .wrapContentHeight()
                .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.extraLarge),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select time",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    fontWeight = FontWeight.Medium
                )

                Box(
                    modifier = Modifier.animateContentSize()
                ) {
                    if (showDial) {
                        TimePicker(
                            state = timePickerState,
                            colors = TimePickerDefaults.colors(
                                clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                                clockDialSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                                clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                                selectorColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    } else {
                        TimeInput(
                            state = timePickerState,
                            colors = TimePickerDefaults.colors()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showDial = !showDial }
                    ) {
                        Icon(
                            imageVector = if (showDial) Icons.Default.Keyboard else Icons.Default.AccessTime,
                            contentDescription = "Toggle input mode",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = MaterialTheme.colorScheme.primary)
                        }
                        TextButton(
                            onClick = {
                                val formatted = String.format(java.util.Locale.US, "%02d:%02d", timePickerState.hour, timePickerState.minute)
                                onTimeSelected(formatted)
                                onDismiss()
                            }
                        ) {
                            Text("OK", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

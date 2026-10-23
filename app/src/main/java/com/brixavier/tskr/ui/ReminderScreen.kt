package com.brixavier.tskr.ui

import kotlin.math.roundToInt
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.key
import com.brixavier.tskr.BuildConfig
import com.brixavier.tskr.model.MonthlyMode
import com.brixavier.tskr.model.RecurrenceDayOfWeek
import com.brixavier.tskr.model.RecurrenceOrdinal
import com.brixavier.tskr.model.RecurrenceRule
import com.brixavier.tskr.model.RecurrenceType
import com.brixavier.tskr.model.YearlyMode
import com.brixavier.tskr.model.PlanCategory
import com.brixavier.tskr.model.CategorySource
import com.brixavier.tskr.model.PlanColor
import com.brixavier.tskr.model.ColorSource
import com.brixavier.tskr.model.PlanCategoryClassifier
import com.brixavier.tskr.model.PlanCategoryColorMapping
import com.brixavier.tskr.ad.AdManager
import com.brixavier.tskr.ad.NativeAdCard
import com.brixavier.tskr.ad.NativeAdEligibility
import com.brixavier.tskr.ui.TskRPlanIcons
import com.brixavier.tskr.ui.PlanColorResolver
import com.brixavier.tskr.ui.theme.TskRMotion
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.brixavier.tskr.bloc.ReminderEvent
import com.brixavier.tskr.ui.theme.getStatusCompletedColor
import com.brixavier.tskr.ui.theme.getStatusOverdueColor
import com.brixavier.tskr.ui.theme.getStatusUpcomingColor
import com.brixavier.tskr.bloc.ReminderState
import com.brixavier.tskr.model.CalendarItem
import com.brixavier.tskr.model.CalendarUtils
import com.brixavier.tskr.model.CalendarViewMode
import com.brixavier.tskr.model.MonthGridDay
import com.brixavier.tskr.model.Plan
import com.brixavier.tskr.model.WeekDay
import com.brixavier.tskr.model.PlanProgress
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.sound.SoundType
import com.brixavier.tskr.ui.theme.*
import com.brixavier.tskr.personality.ActionType
import com.brixavier.tskr.personality.PersonalityContext
import com.brixavier.tskr.personality.PersonalityEngine
import com.brixavier.tskr.personality.UrgencyLevel
import java.time.YearMonth
import android.widget.Toast
import android.text.format.DateFormat
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Screen destinations for dynamic navigation.
 */
enum class Screen {
    Home,
    Tasks,
    Plans,
    Calendar,
    Settings
}

/**
 * Helper to calculate relative time human-readable string.
 * Supports an optional [now] parameter for deterministic unit testing.
 */
fun getRelativeTimeSpan(dateStr: String, timeStr: String, now: Date = Date()): String {
    try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val targetDate = sdf.parse("$dateStr $timeStr") ?: return "$dateStr $timeStr"
        val diffMillis = targetDate.time - now.time
        val diffMinutes = diffMillis / (1000 * 60)

        // Future / Upcoming
        if (diffMinutes >= 0) {
            return when {
                diffMinutes == 0L -> "Starts in less than a minute"
                diffMinutes in 1..59 -> "Starts in $diffMinutes minutes"
                diffMinutes in 60..119 -> "In 1 hour"
                diffMinutes in 120..1439 -> {
                    val hours = (diffMinutes + 30) / 60
                    "In $hours hours"
                }
                else -> {
                    val calDiffDays = (targetDate.time - now.time) / (1000 * 60 * 60 * 24)
                    if (calDiffDays <= 1) "Tomorrow" else "In $calDiffDays days"
                }
            }
        }

        // Past / Overdue
        val overdueMinutes = -diffMinutes
        return when {
            overdueMinutes in 1..119 -> "Overdue by $overdueMinutes minutes"
            overdueMinutes in 120..1439 -> {
                val hours = (overdueMinutes + 30) / 60
                "Overdue by $hours hours"
            }
            else -> {
                val calDiffDays = (now.time - targetDate.time) / (1000 * 60 * 60 * 24)
                if (calDiffDays <= 1) "Yesterday" else "Overdue by $calDiffDays days"
            }
        }
    } catch (_: Exception) {
        return "$dateStr $timeStr"
    }
}

@Composable
fun TopBarHeader(
    title: String,
    subtitle: String? = null,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            if (subtitle != null) {
                AnimatedContent(
                    targetState = subtitle,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(TskRMotion.DURATION_STANDARD)) +
                         slideInVertically(animationSpec = tween(TskRMotion.DURATION_STANDARD), initialOffsetY = { it / 2 })) togetherWith
                        (fadeOut(animationSpec = tween(TskRMotion.DURATION_STANDARD)))
                    },
                    label = "TopBarSubtitleTransition"
                ) { targetSubtitle ->
                    Text(
                        text = targetSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

/**
 * Dynamic layout for the Notes Page displaying active reminder notes and checklists.
 */
@Composable
fun NotesPage(
    state: ReminderState,
    adManager: AdManager? = null,
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
            text = "Because apparently remembering things yourself was too much to ask.",
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Nothing here yet.",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Either you're incredibly organized, or you've forgotten to make a checklist. I'm choosing optimism.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(activeReminders) { reminder ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = reminder.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (reminder.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = reminder.notes,
                                    style = MaterialTheme.typography.bodyMedium,
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

enum class TaskFilter {
    ALL,
    ACTIVE,
    RECURRING,
    COMPLETED,
    OVERDUE
}

/**
 * Tasks Page for managing active missions independently.
 */
@Composable
fun TasksPage(
    state: ReminderState,
    personalityEngine: PersonalityEngine,
    initialFilter: TaskFilter = TaskFilter.ALL,
    adManager: AdManager? = null,
    onEvent: (ReminderEvent) -> Unit,
    onReminderClick: (Reminder) -> Unit = {},
    onEditTemplate: (Reminder) -> Unit,
    onOpenSettings: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember(initialFilter) { mutableStateOf(initialFilter) }

    val canRequestAds = adManager?.canRequestAds?.collectAsState()?.value ?: false
    val isSdkInitialized = adManager?.isInitialized?.collectAsState()?.value ?: false

    val nowStr = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
    }

    val pendingOccMap = remember(state.pendingOccurrences) {
        state.pendingOccurrences.associateBy { it.templateId }
    }

    val completedOccMap = remember(state.completedOccurrences) {
        state.completedOccurrences.associateBy { it.templateId ?: it.id }
    }

    val planMap = remember(state.plans) {
        state.plans.associateBy { it.id }
    }

    // Map active master templates to display their current pending occurrence's scheduled date/time and checklist
    val displayReminders = remember(state.reminders, pendingOccMap) {
        state.reminders.map { rem ->
            val pendingOcc = pendingOccMap[rem.id]
            if (rem.isActive && pendingOcc != null) {
                rem.copy(
                    date = pendingOcc.date,
                    time = pendingOcc.time,
                    subTasksRaw = pendingOcc.subTasksRaw
                )
            } else {
                rem
            }
        }
    }

    val filteredReminders = remember(displayReminders, selectedFilter, searchQuery, nowStr, planMap, completedOccMap) {
        val baseList = when (selectedFilter) {
            TaskFilter.ALL -> displayReminders
            TaskFilter.ACTIVE -> displayReminders.filter { it.isActive }
            TaskFilter.RECURRING -> displayReminders.filter { it.isActive && it.isRecurring() }
            TaskFilter.COMPLETED -> displayReminders.filter { !it.isActive }
            TaskFilter.OVERDUE -> displayReminders.filter { it.isActive && "${it.date} ${it.time}" < nowStr }
        }

        val searchedList = if (searchQuery.isBlank()) {
            baseList
        } else {
            val q = searchQuery.trim().lowercase(Locale.US)
            baseList.filter { reminder ->
                val planName = reminder.planId?.let { planMap[it]?.name } ?: ""
                reminder.title.lowercase(Locale.US).contains(q) ||
                        reminder.notes.lowercase(Locale.US).contains(q) ||
                        reminder.subTasksRaw.lowercase(Locale.US).contains(q) ||
                        planName.lowercase(Locale.US).contains(q)
            }
        }

        // Sorting: Group 0 = Active Overdue, Group 1 = Active Upcoming, Group 2 = Completed
        searchedList.sortedWith { r1, r2 ->
            val group1 = if (!r1.isActive) 2 else if ("${r1.date} ${r1.time}" < nowStr) 0 else 1
            val group2 = if (!r2.isActive) 2 else if ("${r2.date} ${r2.time}" < nowStr) 0 else 1

            if (group1 != group2) {
                return@sortedWith group1.compareTo(group2)
            }

            if (group1 == 2) {
                // Completed group: Most recently completed first
                val c1 = completedOccMap[r1.id]?.completedAtMs ?: 0L
                val c2 = completedOccMap[r2.id]?.completedAtMs ?: 0L
                if (c1 != c2) {
                    return@sortedWith c2.compareTo(c1)
                }
                "${r2.date} ${r2.time}".compareTo("${r1.date} ${r1.time}")
            } else {
                // Active Overdue or Active Upcoming group: Important first, then earliest date/time
                val imp1 = if (r1.isImportant()) 0 else 1
                val imp2 = if (r2.isImportant()) 0 else 1
                if (imp1 != imp2) {
                    return@sortedWith imp1.compareTo(imp2)
                }
                "${r1.date} ${r1.time}".compareTo("${r2.date} ${r2.time}")
            }
        }
    }

    val emptyMessage = remember(selectedFilter, searchQuery, filteredReminders.isEmpty()) {
        when {
            searchQuery.isNotBlank() -> "No tasks matching \"$searchQuery\". Tsk.R checked everywhere."
            selectedFilter == TaskFilter.OVERDUE -> "Zero overdue tasks! You're strictly on schedule. 🎉"
            selectedFilter == TaskFilter.RECURRING -> "No active recurring tasks scheduled."
            selectedFilter == TaskFilter.COMPLETED -> "No completed tasks yet. Time to get to work!"
            selectedFilter == TaskFilter.ACTIVE -> "All active tasks completed! Take a breather or create a new mission."
            else -> {
                val ctx = PersonalityContext(actionType = ActionType.EMPTY_STATE, isAllCaughtUp = true)
                personalityEngine.getMessage(ctx).text
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TopBarHeader(
            title = "Task Library",
            subtitle = "Everything you asked Tsk.R to remember",
            onOpenSettings = onOpenSettings
        )

        // M3 Expressive Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search tasks, notes, plans...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )

        // Filter Chips Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(TaskFilter.entries) { filter ->
                val label = when (filter) {
                    TaskFilter.ALL -> "All (${state.reminders.size})"
                    TaskFilter.ACTIVE -> "Active (${state.reminders.count { it.isActive }})"
                    TaskFilter.RECURRING -> "Recurring (${state.reminders.count { it.isActive && it.isRecurring() }})"
                    TaskFilter.COMPLETED -> "Completed (${state.reminders.count { !it.isActive }})"
                    TaskFilter.OVERDUE -> "Overdue (${state.reminders.count { it.isActive && "${it.date} ${it.time}" < nowStr }})"
                }
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(label, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredReminders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emptyMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val adIndex = NativeAdEligibility.getTaskLibraryAdIndex(filteredReminders.size)
                val showAd = adManager != null && NativeAdEligibility.shouldShowTaskLibraryAd(
                    taskCount = filteredReminders.size,
                    canRequestAds = canRequestAds,
                    isSdkInitialized = isSdkInitialized
                )

                itemsIndexed(filteredReminders, key = { _, item -> item.id }) { index, reminder ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.animateItem()) {
                            ReminderCard(
                                reminder = reminder,
                                plans = state.plans,
                                onClick = { onReminderClick(reminder) },
                                onToggle = { isActive ->
                                    onEvent(ReminderEvent.ToggleReminder(reminder.id, isActive))
                                },
                                onDelete = {
                                    onEvent(ReminderEvent.DeleteReminder(reminder.id))
                                }
                            )
                        }

                        if (showAd && index == adIndex - 1 && adManager != null) {
                            NativeAdCard(adManager = adManager)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Plans Page for higher-level organization and goals.
 */
@Composable
fun PlansPage(
    state: ReminderState,
    personalityEngine: PersonalityEngine,
    adManager: AdManager? = null,
    onEvent: (ReminderEvent) -> Unit,
    onPlanClick: (String) -> Unit,
    onCreatePlanClick: () -> Unit,
    onEditPlanClick: (Plan) -> Unit,
    onOpenSettings: () -> Unit
) {
    val plans = state.plans
    val emptyPlansMessage = remember {
        val ctx = PersonalityContext(actionType = ActionType.EMPTY_STATE, isAllCaughtUp = false)
        personalityEngine.getMessage(ctx).text
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your Plans",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = "Higher-level goals and intentions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (plans.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "No plans yet.",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            Text(
                                text = emptyPlansMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onCreatePlanClick) {
                                Text("Create Your First Plan")
                            }
                        }
                    }
                }
            } else {
                items(plans, key = { it.id }) { plan ->
                    val progress = state.planProgress.find { it.planId == plan.id } ?: PlanProgress(plan.id, 0, 0)
                    val planAccentColor = PlanColorResolver.resolve(plan.resolveEffectiveColor(), isSystemInDarkTheme())
                    val animatedPlanProgress by animateFloatAsState(
                        targetValue = progress.progressRatio,
                        animationSpec = tween(durationMillis = TskRMotion.DURATION_STANDARD, easing = TskRMotion.EasingStandard),
                        label = "PlanCardProgressAnimation"
                    )

                    Card(
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .clickable { onPlanClick(plan.id) },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        ),
                        border = BorderStroke(1.5.dp, planAccentColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val planIcon = TskRPlanIcons.getIcon(plan.getPlanCategory())
                                    val planAccentColor = PlanColorResolver.resolve(plan.resolveEffectiveColor(), isSystemInDarkTheme())
                                    Icon(
                                        imageVector = planIcon,
                                        contentDescription = null,
                                        tint = planAccentColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = plan.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (plan.description.isNotBlank()) {
                                Text(
                                    text = plan.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (progress.totalTasks == 0) "No steps yet" else "${progress.completedTasks} / ${progress.totalTasks} steps completed",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (!plan.deadline.isNullOrBlank()) {
                                        Text(
                                            text = "Due: ${plan.deadline}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                if (progress.totalTasks > 0) {
                                    LinearProgressIndicator(
                                        progress = { animatedPlanProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp),
                                        color = MaterialTheme.colorScheme.tertiary,
                                        trackColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
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

data class CalendarDay(
    val formatted: String,
    val dayName: String,
    val dayNum: Int
)

/**
 * 7-day Week View Strip composable for Calendar 2.0.
 */
@Composable
fun WeekViewStrip(
    selectedDate: LocalDate,
    calendarItems: List<CalendarItem>,
    onDateSelected: (LocalDate) -> Unit
) {
    val weekDays = remember(selectedDate) {
        CalendarUtils.getWeekDays(selectedDate)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        weekDays.forEach { weekDay ->
            val isSelected = weekDay.date == selectedDate
            val dateStr = weekDay.date.toString()
            val dayTasks = remember(calendarItems, dateStr) {
                calendarItems.filter { it.date == dateStr && it.isActive }
            }
            val hasOverdue = dayTasks.any { it.isOverdue }
            val hasImportant = dayTasks.any { it.isImportant() }
            val taskCount = CalendarUtils.taskCountForDate(calendarItems, weekDay.date)
            val dotCount = CalendarUtils.getDensityDotCount(calendarItems, weekDay.date)
            val cellAccessibilityLabel = CalendarUtils.getDateCellContentDescription(
                date = weekDay.date,
                taskCount = taskCount,
                isSelected = isSelected,
                isToday = weekDay.isToday
            )

            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(84.dp)
                    .semantics { contentDescription = cellAccessibilityLabel }
                    .clickable { onDateSelected(weekDay.date) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        weekDay.isToday -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = weekDay.dayName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            weekDay.isToday -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        }
                    )

                    Text(
                        text = weekDay.dayNum.toString(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.onPrimary
                            weekDay.isToday -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )

                    // 0 / 1 / 2 / 3+ Task-Density Indicator Dots
                    if (dotCount > 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val dotColor = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                hasOverdue || hasImportant -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.primary
                            }

                            repeat(dotCount) { dotIdx ->
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(
                                            color = if (dotIdx == 0) dotColor else dotColor.copy(alpha = 0.8f),
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    } else {
                        Box(modifier = Modifier.size(4.dp))
                    }
                }
            }
        }
    }
}

/**
 * 7-column Month View Grid composable for Calendar 2.0.
 */
@Composable
fun MonthViewGrid(
    displayedYearMonth: YearMonth,
    selectedDate: LocalDate,
    calendarItems: List<CalendarItem>,
    onDateSelected: (LocalDate) -> Unit
) {
    val monthGridDays = remember(displayedYearMonth) {
        CalendarUtils.generateMonthGrid(displayedYearMonth)
    }

    val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Weekday Headers Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            weekdays.forEach { dayName ->
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 7-Column Grid of Days
        val rows = remember(monthGridDays) { monthGridDays.chunked(7) }
        rows.forEach { weekRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                weekRow.forEach { dayCell ->
                    val isSelected = dayCell.date == selectedDate
                    val dateStr = dayCell.date.toString()
                    val dayTasks = remember(calendarItems, dateStr) {
                        calendarItems.filter { it.date == dateStr && it.isActive }
                    }
                    val hasOverdue = dayTasks.any { it.isOverdue }
                    val hasImportant = dayTasks.any { it.isImportant() }
                    val taskCount = CalendarUtils.taskCountForDate(calendarItems, dayCell.date)
                    val dotCount = CalendarUtils.getDensityDotCount(calendarItems, dayCell.date)
                    val cellAccessibilityLabel = CalendarUtils.getDateCellContentDescription(
                        date = dayCell.date,
                        taskCount = taskCount,
                        isSelected = isSelected,
                        isToday = dayCell.isToday
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    dayCell.isToday -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                            )
                            .semantics { contentDescription = cellAccessibilityLabel }
                            .clickable { onDateSelected(dayCell.date) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = dayCell.date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (dayCell.isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    !dayCell.isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )

                            // 0 / 1 / 2 / 3+ Task-Density Indicator Dots
                            if (dotCount > 0) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    val dotColor = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        hasOverdue || hasImportant -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.primary
                                    }

                                    repeat(dotCount) { dotIdx ->
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .background(
                                                    color = if (dotIdx == 0) dotColor else dotColor.copy(alpha = 0.8f),
                                                    shape = CircleShape
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
}

/**
 * Chronological Day View Timeline composable for Calendar 2.0.
 */
@Composable
fun DayViewTimeline(
    selectedDate: LocalDate,
    dayItems: List<CalendarItem>,
    state: ReminderState,
    onReminderClick: (Reminder) -> Unit,
    onEvent: (ReminderEvent) -> Unit
) {
    val sortedDayItems = remember(dayItems) {
        dayItems.sortedWith(
            compareBy<CalendarItem> { it.time }
                .thenBy { if (it.isImportant()) 0 else 1 }
                .thenBy { it.title }
        )
    }

    val selectedDateStr = selectedDate.toString()
    val fullDateHeader = remember(selectedDate) {
        selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy", Locale.US))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (sortedDayItems.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
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
                        imageVector = Icons.Default.EventAvailable,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "No Commitments Scheduled",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Your schedule for $fullDateHeader is completely clear.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                sortedDayItems.forEach { item ->
                    val masterTemplate = state.reminders.find { it.id == item.templateId }
                    if (masterTemplate != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = item.time,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (item.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(52.dp)
                            )

                            Box(modifier = Modifier.weight(1f)) {
                                ReminderCard(
                                    reminder = masterTemplate.copy(
                                        date = item.date,
                                        time = item.time,
                                        subTasksRaw = item.subTasksRaw
                                    ),
                                    plans = state.plans,
                                    onClick = { onReminderClick(masterTemplate) },
                                    onToggle = { isActive ->
                                        onEvent(ReminderEvent.ToggleReminder(item.occurrenceId ?: masterTemplate.id, isActive))
                                    },
                                    onDelete = {
                                        onEvent(ReminderEvent.DeleteReminder(masterTemplate.id))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dynamic layout for the Calendar Page displaying a premium daily planner and schedule.
 */
@Composable
fun CalendarPage(
    state: ReminderState,
    personalityEngine: PersonalityEngine,
    adManager: AdManager? = null,
    onEvent: (ReminderEvent) -> Unit,
    onReminderClick: (Reminder) -> Unit = {},
    onOpenSettings: () -> Unit
) {
    var viewMode by remember { mutableStateOf(CalendarViewMode.MONTH) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var displayedYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var slideDirection by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    fun selectDate(newDate: LocalDate) {
        selectedDate = newDate
        displayedYearMonth = YearMonth.from(newDate)
    }

    val nowStr = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
    }

    val calendarItems = remember(state.reminders, state.pendingOccurrences, state.plans, nowStr) {
        CalendarUtils.mapToCalendarItems(state.reminders, state.pendingOccurrences, state.plans, nowStr)
    }

    val dateRange = remember(viewMode, selectedDate, displayedYearMonth) {
        when (viewMode) {
            CalendarViewMode.MONTH -> CalendarUtils.getMonthRange(displayedYearMonth)
            CalendarViewMode.WEEK -> CalendarUtils.getWeekRange(selectedDate)
            CalendarViewMode.DAY -> CalendarUtils.getDayRange(selectedDate)
        }
    }

    val selectedDateStr = selectedDate.toString()
    val dayItems = remember(calendarItems, selectedDateStr) {
        calendarItems.filter { it.date == selectedDateStr }
    }

    val calendarSubtitle = remember {
        val ctx = PersonalityContext(actionType = ActionType.WIDGET)
        personalityEngine.getMessage(ctx).text
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TopBarHeader(
            title = "Your Schedule",
            subtitle = calendarSubtitle,
            onOpenSettings = onOpenSettings
        )

        // View Mode Switcher Chips & Navigation Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CalendarViewMode.entries.forEach { mode ->
                    val label = when (mode) {
                        CalendarViewMode.MONTH -> "Month"
                        CalendarViewMode.WEEK -> "Week"
                        CalendarViewMode.DAY -> "Day"
                    }
                    FilterChip(
                        selected = viewMode == mode,
                        onClick = { viewMode = mode },
                        label = { Text(label, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(onClick = {
                    slideDirection = -1
                    when (viewMode) {
                        CalendarViewMode.MONTH -> {
                            val nextYm = displayedYearMonth.minusMonths(1)
                            selectDate(nextYm.atDay(1))
                        }
                        CalendarViewMode.WEEK -> selectDate(selectedDate.minusWeeks(1))
                        CalendarViewMode.DAY -> selectDate(selectedDate.minusDays(1))
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                }

                TextButton(onClick = {
                    slideDirection = if (LocalDate.now().isAfter(selectedDate)) 1 else -1
                    selectDate(LocalDate.now())
                }) {
                    Text("Today", fontWeight = FontWeight.Bold)
                }

                IconButton(onClick = {
                    slideDirection = 1
                    when (viewMode) {
                        CalendarViewMode.MONTH -> {
                            val nextYm = displayedYearMonth.plusMonths(1)
                            selectDate(nextYm.atDay(1))
                        }
                        CalendarViewMode.WEEK -> selectDate(selectedDate.plusWeeks(1))
                        CalendarViewMode.DAY -> selectDate(selectedDate.plusDays(1))
                    }
                }) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Next")
                }
            }
        }

        // Active Range Display
        val rangeTitle = remember(viewMode, selectedDate, displayedYearMonth) {
            when (viewMode) {
                CalendarViewMode.MONTH -> displayedYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
                CalendarViewMode.WEEK -> "Week of ${dateRange.startDate.format(DateTimeFormatter.ofPattern("MMM dd", Locale.US))}"
                CalendarViewMode.DAY -> selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy", Locale.US))
            }
        }

        // Month / Week View Containers with directional transition
        AnimatedContent(
            targetState = Triple(viewMode, displayedYearMonth, selectedDate),
            transitionSpec = {
                val dir = slideDirection
                val slidePx = with(density) { 12.dp.roundToPx() }
                if (targetState.first != initialState.first) {
                    TskRMotion.tabTransition()
                } else {
                    if (dir >= 0) {
                        (slideInHorizontally(animationSpec = tween(TskRMotion.DURATION_STANDARD), initialOffsetX = { slidePx }) + fadeIn(animationSpec = tween(TskRMotion.DURATION_STANDARD))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(TskRMotion.DURATION_STANDARD), targetOffsetX = { -slidePx }) + fadeOut(animationSpec = tween(TskRMotion.DURATION_STANDARD)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(TskRMotion.DURATION_STANDARD), initialOffsetX = { -slidePx }) + fadeIn(animationSpec = tween(TskRMotion.DURATION_STANDARD))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(TskRMotion.DURATION_STANDARD), targetOffsetX = { slidePx }) + fadeOut(animationSpec = tween(TskRMotion.DURATION_STANDARD)))
                    }
                }
            },
            label = "CalendarPeriodTransition",
            modifier = Modifier.fillMaxWidth()
        ) { stateTriple ->
            val vm = stateTriple.first
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = rangeTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (vm == CalendarViewMode.MONTH) {
                    MonthViewGrid(
                        displayedYearMonth = displayedYearMonth,
                        selectedDate = selectedDate,
                        calendarItems = calendarItems,
                        onDateSelected = { selectDate(it) }
                    )
                } else if (vm == CalendarViewMode.WEEK) {
                    WeekViewStrip(
                        selectedDate = selectedDate,
                        calendarItems = calendarItems,
                        onDateSelected = { selectDate(it) }
                    )
                }
            }
        }

        // Selected-Date Agenda Section Header
        val selectedDateHeader = remember(selectedDate) {
            selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Schedule for $selectedDateHeader",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (dayItems.isEmpty()) "0 tasks" else "${dayItems.size} task${if (dayItems.size > 1) "s" else ""}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Day View Timeline / Agenda Container
        DayViewTimeline(
            selectedDate = selectedDate,
            dayItems = dayItems,
            state = state,
            onReminderClick = onReminderClick,
            onEvent = onEvent
        )
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
    personalityEngine: PersonalityEngine,
    initialScreen: Screen = Screen.Home,
    initialShowBottomSheet: Boolean = false,
    preselectedPlanId: String? = null,
    initialEditingReminderId: String? = null,
    isExactAlarmAllowed: Boolean = true,
    isBatteryOptimizationIgnored: Boolean = true,
    appVersion: String = BuildConfig.VERSION_NAME,
    onOpenAlarmSettings: () -> Unit = {},
    onOpenBatterySettings: () -> Unit = {},
    onPickSound: (SoundType) -> Unit = {},
    onResetSound: (SoundType) -> Unit = {},
    getSoundTitle: (SoundType) -> String = { "System Default" },
    onOpenWhatsNew: () -> Unit = {},
    currentThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    isPrivacyOptionsRequired: Boolean = false,
    onOpenPrivacyOptions: () -> Unit = {},
    onRateApp: () -> Unit = {},
    onThemeModeChange: (AppThemeMode) -> Unit = {},
    adManager: AdManager? = null,
    onEvent: (ReminderEvent) -> Unit,
    onReminderClick: (Reminder) -> Unit = {},
    onPlanClick: (String) -> Unit = {},
    onTabSelected: (Screen) -> Unit = {},
    onSheetClosed: () -> Unit = {}
) {
    if (state == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val canRequestAds = adManager?.canRequestAds?.collectAsState()?.value ?: false
    val isSdkInitialized = adManager?.isInitialized?.collectAsState()?.value ?: false

    var showBottomSheet by remember { mutableStateOf(initialShowBottomSheet) }
    var editingReminder by remember {
        mutableStateOf<Reminder?>(
            if (initialEditingReminderId != null) state.reminders.find { it.id == initialEditingReminderId } else null
        )
    }
    var reminderPlanId by remember { mutableStateOf(preselectedPlanId) }
    var showPlanSheet by remember { mutableStateOf(false) }
    var editingPlan by remember { mutableStateOf<Plan?>(null) }
    var showCreateMenu by remember { mutableStateOf(false) }
    var isHistoryExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val planSheetState = rememberModalBottomSheetState()
    var currentScreen by remember(initialScreen) { mutableStateOf(initialScreen) }
    var tasksInitialFilter by remember { mutableStateOf(TaskFilter.ALL) }

    val homeLazyListState = rememberLazyListState()
    val isFabExpanded by remember {
        derivedStateOf {
            homeLazyListState.firstVisibleItemIndex == 0
        }
    }

    // Intercept system Back button on non-Home screens to return to Home
    BackHandler(enabled = currentScreen != Screen.Home) {
        currentScreen = Screen.Home
    }

    // Dynamically calculate greeting based on local clock
    val (greeting, icon) = remember {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good morning" to "☀️"
            in 12..16 -> "Good afternoon" to "🌤️"
            else -> "Good evening" to "🌙"
        }
    }

    // Split reminders into active list
    val activeReminders = remember(state.reminders) {
        state.reminders.filter { it.isActive }.sortedBy { "${it.date} ${it.time}" }
    }

    // Daily progress calculation based on today's workload (and active overdue tasks)
    val todayDate = remember { LocalDate.now().toString() }
    val todaysReminders = remember(state.reminders, todayDate) {
        state.reminders.filter { it.date == todayDate || (it.isActive && it.date < todayDate) }
    }
    val todaysTotal = todaysReminders.size
    val todaysActive = remember(todaysReminders) { todaysReminders.count { it.isActive } }
    val todaysCompleted = todaysTotal - todaysActive
    val progress = if (todaysTotal > 0) todaysCompleted.toFloat() / todaysTotal else 0f

    val nextUp = activeReminders.firstOrNull()
    val randomQuote = remember {
        val context = PersonalityContext(actionType = ActionType.EMPTY_STATE, isAllCaughtUp = true)
        personalityEngine.getMessage(context).text
    }

    val homeSubtitle = remember {
        val context = PersonalityContext(actionType = ActionType.WIDGET)
        personalityEngine.getMessage(context).text
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { TskRMotion.tabTransition() },
                label = "TabScreenTransition",
                modifier = Modifier.fillMaxSize()
            ) { targetScreen ->
                when (targetScreen) {
                    Screen.Home -> {
                        LazyColumn(
                        state = homeLazyListState,
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 16.dp,
                            bottom = 100.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .animateContentSize()
                    ) {
                        // 1. Centered Header & Greeting
                        item {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                IconButton(
                                    onClick = { currentScreen = Screen.Settings },
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Tsk.R: Procrastinateless",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$greeting $icon",
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        ),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = homeSubtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )

                                    if (state.streak > 0) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.LocalFireDepartment,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "${state.streak} DAY STREAK",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Weekly Progress Card (Hero Container)
                        item {
                            val wp = state.weeklyProgress
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(28.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 18.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Weekly Progress",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "${wp.progressPercent}%",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    val animatedWpProgress by animateFloatAsState(
                                        targetValue = wp.progressRatio,
                                        animationSpec = tween(durationMillis = TskRMotion.DURATION_STANDARD, easing = TskRMotion.EasingStandard),
                                        label = "WeeklyProgressAnimation"
                                    )
                                    LinearProgressIndicator(
                                        progress = { animatedWpProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = if (wp.totalScheduledCount > 0) {
                                            "${wp.completedCount} of ${wp.totalScheduledCount} scheduled tasks completed this week"
                                        } else {
                                            "No tasks scheduled for this week."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }

                        // 3. At a Glance Summary Cards (Shortcuts)
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "At a Glance",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                )

                                val pendingCount = state.uiItems.count { it.isPending() }
                                val recurringCount = state.reminders.count { it.isRecurring() && it.isActive }
                                val activePlansCount = state.plans.size

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Pending Card Shortcut
                                    Surface(
                                        onClick = {
                                            tasksInitialFilter = TaskFilter.ACTIVE
                                            currentScreen = Screen.Tasks
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.TaskAlt,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Text(
                                                text = "$pendingCount",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "Pending",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Recurring Card Shortcut
                                    Surface(
                                        onClick = {
                                            tasksInitialFilter = TaskFilter.RECURRING
                                            currentScreen = Screen.Tasks
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Repeat,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Text(
                                                text = "$recurringCount",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "Recurring",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Active Plans Shortcut
                                    Surface(
                                        onClick = { currentScreen = Screen.Plans },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Flag,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Text(
                                                text = "$activePlansCount",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "Plans",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Coming Up Next Section
                        val upcomingItems = state.uiItems.filter { it.isPending() }.take(2)
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Coming Up Next",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                )

                                if (upcomingItems.isNotEmpty()) {
                                    upcomingItems.forEach { item ->
                                        Surface(
                                            onClick = {
                                                val tmpl = state.reminders.find { it.id == item.templateId || it.id == item.occurrenceId }
                                                if (tmpl != null) onReminderClick(tmpl)
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (item.isImportant()) Icons.Default.NotificationsActive else Icons.Default.Schedule,
                                                    contentDescription = null,
                                                    tint = if (item.isImportant()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.title,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = getRelativeTimeSpan(item.date, item.time),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.Default.ChevronRight,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "No upcoming tasks right now.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        // 5. Recurring Tasks Section
                        val recurringItems = state.uiItems.filter { it.isRecurring() && it.isPending() }.take(2)
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Recurring Tasks",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                        )
                                    )
                                    TextButton(
                                        onClick = {
                                            tasksInitialFilter = TaskFilter.RECURRING
                                            currentScreen = Screen.Tasks
                                        },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("See All")
                                    }
                                }

                                if (recurringItems.isNotEmpty()) {
                                    recurringItems.forEach { item ->
                                        Surface(
                                            onClick = {
                                                val tmpl = state.reminders.find { it.id == item.templateId || it.id == item.occurrenceId }
                                                if (tmpl != null) onReminderClick(tmpl)
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Repeat,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.title,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${item.getRecurrenceCleanName()} • ${getRelativeTimeSpan(item.date, item.time)}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.Default.ChevronRight,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    DashboardEmptyStateCard(
                                        title = "No recurring routines yet",
                                        prompt = "Consistency is kryptonite to procrastination.",
                                        buttonText = "+ Add",
                                        onClick = {
                                            tasksInitialFilter = TaskFilter.RECURRING
                                            currentScreen = Screen.Tasks
                                        }
                                    )
                                }
                            }
                        }

                        // 6. Plans Section
                        val activePlans = state.plans.take(2)
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Plans",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                        )
                                    )
                                    TextButton(
                                        onClick = { currentScreen = Screen.Plans },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("See All")
                                    }
                                }

                                if (activePlans.isNotEmpty()) {
                                    activePlans.forEach { plan ->
                                        val progress = state.planProgress.find { it.planId == plan.id } ?: PlanProgress(plan.id, 0, 0)
                                        val planIcon = TskRPlanIcons.getIcon(plan.getPlanCategory())
                                        val planAccentColor = PlanColorResolver.resolve(plan.resolveEffectiveColor(), isSystemInDarkTheme())

                                        Surface(
                                            onClick = { currentScreen = Screen.Plans },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                            border = BorderStroke(1.dp, planAccentColor.copy(alpha = 0.6f))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(
                                                            imageVector = planIcon,
                                                            contentDescription = null,
                                                            tint = planAccentColor,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Text(
                                                            text = plan.name,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = if (progress.totalTasks > 0) "${(progress.progressRatio * 100).roundToInt()}%" else "0%",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1
                                                    )
                                                }

                                                val animatedDashboardPlanProgress by animateFloatAsState(
                                                    targetValue = progress.progressRatio,
                                                    animationSpec = tween(durationMillis = TskRMotion.DURATION_STANDARD, easing = TskRMotion.EasingStandard),
                                                    label = "DashboardPlanProgressAnimation"
                                                )
                                                LinearProgressIndicator(
                                                    progress = { animatedDashboardPlanProgress },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(6.dp),
                                                    color = planAccentColor,
                                                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                    strokeCap = StrokeCap.Round
                                                )

                                                Text(
                                                    text = if (progress.totalTasks == 0) "No steps yet" else "${progress.completedTasks} of ${progress.totalTasks} steps completed",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    DashboardEmptyStateCard(
                                        title = "No active plans",
                                        prompt = "Break big goals into simple steps.",
                                        buttonText = "+ Create",
                                        onClick = {
                                            editingPlan = null
                                            showPlanSheet = true
                                        }
                                    )
                                }
                            }
                        }

                        // 7. Single Native Ad Placement on Dashboard (Placed after Plans section at bottom of main content)
                        if (adManager != null && NativeAdEligibility.shouldShowDashboardAd(canRequestAds, isSdkInitialized)) {
                            item {
                                NativeAdCard(adManager = adManager)
                            }
                        }
                    }
                }
                Screen.Tasks -> {
                    TasksPage(
                        state = state,
                        personalityEngine = personalityEngine,
                        adManager = adManager,
                        initialFilter = tasksInitialFilter,
                        onEvent = onEvent,
                        onReminderClick = onReminderClick,
                        onEditTemplate = { reminder ->
                            editingReminder = reminder
                            showBottomSheet = true
                        },
                        onOpenSettings = { currentScreen = Screen.Settings }
                    )
                }
                Screen.Plans -> {
                    PlansPage(
                        state = state,
                        personalityEngine = personalityEngine,
                        onEvent = onEvent,
                        onPlanClick = onPlanClick,
                        onCreatePlanClick = {
                            editingPlan = null
                            showPlanSheet = true
                        },
                        onEditPlanClick = { plan ->
                            editingPlan = plan
                            showPlanSheet = true
                        },
                        onOpenSettings = { currentScreen = Screen.Settings }
                    )
                }
                Screen.Calendar -> {
                    CalendarPage(state = state, personalityEngine = personalityEngine, onEvent = onEvent, onOpenSettings = { currentScreen = Screen.Settings })
                }
                Screen.Settings -> {
                    SettingsPage(
                        isExactAlarmAllowed = isExactAlarmAllowed,
                        isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
                        appVersion = appVersion,
                        currentThemeMode = currentThemeMode,
                        isPrivacyOptionsRequired = isPrivacyOptionsRequired,
                        onOpenAlarmSettings = onOpenAlarmSettings,
                        onOpenBatterySettings = onOpenBatterySettings,
                        onPickSound = onPickSound,
                        onResetSound = onResetSound,
                        getSoundTitle = getSoundTitle,
                        onOpenWhatsNew = onOpenWhatsNew,
                        onOpenPrivacyOptions = onOpenPrivacyOptions,
                        onRateApp = onRateApp,
                        onThemeModeChange = onThemeModeChange
                    )
                }
            }
            }

            // V1-inspired Pill Navigation Bar + Universal "+" Action Button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.wrapContentSize()
                ) {
                    // Refined Floating Navigation Pill
                    Surface(
                        modifier = Modifier
                            .height(64.dp)
                            .shadow(8.dp, CircleShape),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 4.dp
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
                                Triple(Screen.Tasks, Icons.Default.CheckCircle, "Tasks"),
                                Triple(Screen.Plans, Icons.Default.Flag, "Plans"),
                                Triple(Screen.Calendar, Icons.Default.CalendarToday, "Calendar")
                            )

                            val tabAnimatable = remember { Animatable(1f) }
                            var previousScreen by remember { mutableStateOf(currentScreen) }
                            val density = LocalDensity.current
                            val arcHeightPx = with(density) { 12.dp.toPx() }

                            LaunchedEffect(currentScreen) {
                                if (previousScreen != currentScreen) {
                                    tabAnimatable.snapTo(0f)
                                    tabAnimatable.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(durationMillis = TskRMotion.DURATION_STANDARD, easing = TskRMotion.EasingStandard)
                                    )
                                    previousScreen = currentScreen
                                }
                            }

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

                                val t = if (selected) tabAnimatable.value else 1f
                                val arcY = -4f * arcHeightPx * t * (1f - t)

                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(animatedContainerColor)
                                        .graphicsLayer {
                                            translationY = if (selected) arcY else 0f
                                        }
                                        .clickable {
                                            if (screen == Screen.Tasks) tasksInitialFilter = TaskFilter.ALL
                                            currentScreen = screen
                                            onTabSelected(screen)
                                        },
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

                    val fabInteractionSource = remember { MutableInteractionSource() }
                    val isFabPressed by fabInteractionSource.collectIsPressedAsState()
                    val fabScale by animateFloatAsState(
                        targetValue = if (isFabPressed) 0.97f else 1.0f,
                        animationSpec = tween(durationMillis = TskRMotion.DURATION_MICRO, easing = TskRMotion.EasingStandard),
                        label = "FabPressScaleAnimation"
                    )

                    // Universal "+" Action Button
                    FloatingActionButton(
                        onClick = { showCreateMenu = true },
                        interactionSource = fabInteractionSource,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp, pressedElevation = 8.dp),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(64.dp)
                            .graphicsLayer {
                                scaleX = fabScale
                                scaleY = fabScale
                            }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create", modifier = Modifier.size(28.dp))
                    }
                }
            }

            if (showCreateMenu) {
                val createChooserSheetState = rememberModalBottomSheetState()
                val menuAnimatable = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    menuAnimatable.snapTo(0f)
                    menuAnimatable.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = TskRMotion.DURATION_STANDARD, easing = TskRMotion.EasingStandard)
                    )
                }
                val t = menuAnimatable.value
                val scale = 0.7f + 0.3f * t
                val alpha = t
                val (arcX, arcY) = TskRMotion.calculateParabolicOffset(t, startX = 24f * (1f - t), endX = 0f, startY = 24f * (1f - t), endY = 0f, arcHeightPx = 12f)

                ModalBottomSheet(
                    onDismissRequest = { showCreateMenu = false },
                    sheetState = createChooserSheetState,
                    containerColor = MaterialTheme.colorScheme.surface,
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                                translationX = arcX
                                translationY = arcY
                            }
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "What would you like to create?",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Choose what you'd like to add to Tsk.R.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Task (Reminder) Card
                        Surface(
                            onClick = {
                                showCreateMenu = false
                                editingReminder = null
                                reminderPlanId = null
                                showBottomSheet = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TaskAlt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Task (Reminder)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Something you need to get done.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        // Plan (Goal) Card
                        Surface(
                            onClick = {
                                showCreateMenu = false
                                editingPlan = null
                                showPlanSheet = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Flag,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Plan (Goal)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "A bigger goal you're working toward.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showCreateMenu = false }) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }

            // Add Reminder Modal Bottom Sheet
            if (showBottomSheet) {
                AddReminderBottomSheet(
                    reminderToEdit = editingReminder,
                    plans = state.plans,
                    preselectedPlanId = reminderPlanId,
                    sheetState = sheetState,
                    onDismiss = { 
                        showBottomSheet = false
                        editingReminder = null
                        reminderPlanId = null
                        onSheetClosed()
                    },
                    onSave = { title, date, time, priority, notes, subTasksRaw, motivation, recType, recInterval, planId, n7d, n24h, nNow, recDays, recMonthlyMode, recYearlyMode, recOrdinal, recWeekday, recYearlyMonth ->
                        val editRem = editingReminder
                        if (editRem != null) {
                            onEvent(ReminderEvent.UpdateReminder(editRem.copy(
                                title = title,
                                date = date,
                                time = time,
                                priority = priority,
                                notes = notes,
                                subTasksRaw = subTasksRaw,
                                motivation = motivation,
                                recurrenceType = recType,
                                recurrenceInterval = recInterval,
                                planId = planId,
                                notify7Days = n7d,
                                notify24Hours = n24h,
                                notifyDueNow = nNow,
                                recurrenceDaysOfWeek = recDays,
                                recurrenceMonthlyMode = recMonthlyMode,
                                recurrenceYearlyMode = recYearlyMode,
                                recurrenceOrdinal = recOrdinal,
                                recurrenceWeekday = recWeekday,
                                recurrenceYearlyMonth = recYearlyMonth
                            )))
                        } else {
                            onEvent(ReminderEvent.AddReminder(
                                title = title,
                                date = date,
                                time = time,
                                priority = priority,
                                notes = notes,
                                subTasksRaw = subTasksRaw,
                                motivation = motivation,
                                recurrenceType = recType,
                                recurrenceInterval = recInterval,
                                planId = planId,
                                notify7Days = n7d,
                                notify24Hours = n24h,
                                notifyDueNow = nNow,
                                recurrenceDaysOfWeek = recDays,
                                recurrenceMonthlyMode = recMonthlyMode,
                                recurrenceYearlyMode = recYearlyMode,
                                recurrenceOrdinal = recOrdinal,
                                recurrenceWeekday = recWeekday,
                                recurrenceYearlyMonth = recYearlyMonth
                            ))
                        }
                        showBottomSheet = false
                        editingReminder = null
                        reminderPlanId = null
                        onSheetClosed()
                    }
                )
            }

            if (showPlanSheet) {
                AddPlanBottomSheet(
                    planToEdit = editingPlan,
                    sheetState = planSheetState,
                    onDismiss = {
                        showPlanSheet = false
                        editingPlan = null
                    },
                    onSave = { name, description, icon, deadline, category, categorySource, color, colorSource ->
                        val editPln = editingPlan
                        if (editPln != null) {
                            onEvent(ReminderEvent.UpdatePlan(editPln.copy(
                                name = name,
                                description = description,
                                icon = icon,
                                deadline = deadline,
                                category = category,
                                categorySource = categorySource,
                                color = color,
                                colorSource = colorSource
                            )))
                        } else {
                            onEvent(ReminderEvent.AddPlan(
                                name = name,
                                description = description,
                                icon = icon,
                                deadline = deadline,
                                category = category,
                                categorySource = categorySource,
                                color = color,
                                colorSource = colorSource
                            ))
                        }
                        showPlanSheet = false
                        editingPlan = null
                    }
                )
            }
        }
    }
}

@Composable
fun DashboardEmptyStateCard(
    title: String,
    prompt: String,
    buttonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = prompt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(onClick = onClick) {
                Text(
                    text = buttonText,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Refined compact Task Card for Task Library rendering.
 * Features dense information layout, intentional Material iconography, and responsive typography.
 */
@Composable
fun ReminderCard(
    reminder: Reminder,
    plans: List<Plan> = emptyList(),
    nowStr: String = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date()) },
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit = {}
) {
    val isCompleted = !reminder.isActive
    val isOverdue = reminder.isActive && "${reminder.date} ${reminder.time}" < nowStr
    val isImportant = reminder.isImportant()

    val alpha = if (isCompleted) 0.6f else 1f
    val strikeThrough = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None

    val isDark = isSystemInDarkTheme()
    val statusColor = when {
        isCompleted -> getStatusCompletedColor(isDark)
        isOverdue || isImportant -> getStatusOverdueColor(isDark)
        else -> getStatusUpcomingColor(isDark)
    }

    val plan = remember(reminder.planId, plans) {
        reminder.planId?.let { pId -> plans.find { it.id == pId } }
    }

    val cleanRecurrence = remember(reminder.recurrenceType, reminder.recurrenceInterval) {
        when (reminder.recurrenceType) {
            Reminder.RECURRENCE_DAILY -> if (reminder.recurrenceInterval == 1) "Daily" else "Every ${reminder.recurrenceInterval} days"
            Reminder.RECURRENCE_WEEKLY -> if (reminder.recurrenceInterval == 1) "Weekly" else "Every ${reminder.recurrenceInterval} weeks"
            Reminder.RECURRENCE_MONTHLY -> if (reminder.recurrenceInterval == 1) "Monthly" else "Every ${reminder.recurrenceInterval} months"
            Reminder.RECURRENCE_CUSTOM_DAYS -> "Every ${reminder.recurrenceInterval} days"
            else -> null
        }
    }

    val targetContainerColor = if (isCompleted) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
    }
    val animatedContainerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = tween(durationMillis = TskRMotion.DURATION_MICRO, easing = TskRMotion.EasingStandard),
        label = "TaskCardBackgroundColorAnimation"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isCompleted) 1.1f else 1.0f,
        animationSpec = tween(durationMillis = TskRMotion.DURATION_MICRO, easing = TskRMotion.EasingStandard),
        label = "TaskCompletionScaleAnimation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = animatedContainerColor
        ),
        border = if (reminder.isActive) {
            BorderStroke(1.dp, statusColor.copy(alpha = 0.25f))
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Status Strip
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(statusColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Task Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isImportant && reminder.isActive) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Important",
                                tint = statusColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = reminder.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                textDecoration = strikeThrough
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    // Metadata Row: Date/Time + Recurrence + Plan + Checklist
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Date / Relative Time
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isOverdue) Icons.Default.PriorityHigh else Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = if (isOverdue) statusColor else statusColor.copy(alpha = alpha),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (reminder.isActive) getRelativeTimeSpan(reminder.date, reminder.time) else "${reminder.date} ${reminder.time}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isOverdue) statusColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Recurrence Badge
                        if (cleanRecurrence != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = alpha),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = cleanRecurrence,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = alpha),
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }

                        // Plan Indicator
                        if (plan != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = plan.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Checklist Progress
                        if (reminder.subTasksRaw.isNotBlank()) {
                            val steps = reminder.subTasksRaw.split("|").filter { it.isNotBlank() }
                            val completedSteps = steps.count { it.trim().startsWith("[x] ") }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "$completedSteps/${steps.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Optional Notes Snippet
                    if (reminder.notes.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notes,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = alpha),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = reminder.notes,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha * 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Trailing Completion Action Control (Direct Trash Icon Removed for Safety)
                IconButton(
                    onClick = { onToggle(!reminder.isActive) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Toggle completion",
                        tint = if (isCompleted) statusColor else MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            }
                    )
                }
            }
        }
    }
}

fun buildRecurrenceSummary(rule: RecurrenceRule): String {
    if (!rule.isRecurring()) return "No repeat"

    return when (rule.type) {
        RecurrenceType.NONE -> "No repeat"
        RecurrenceType.DAILY -> if (rule.interval == 1) "Repeats daily" else "Repeats every ${rule.interval} days"
        RecurrenceType.WEEKLY -> {
            val days = if (rule.daysOfWeek.isEmpty()) "selected days" else rule.getSerializedDaysOfWeek().replace("|", ", ")
            if (rule.interval == 1) "Repeats weekly on $days" else "Repeats every ${rule.interval} weeks on $days"
        }
        RecurrenceType.MONTHLY -> {
            if (rule.monthlyMode == MonthlyMode.ORDINAL_WEEKDAY) {
                val ord = rule.ordinal?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "first"
                val day = rule.weekday?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Monday"
                if (rule.interval == 1) "Repeats monthly on the $ord $day" else "Repeats every ${rule.interval} months on the $ord $day"
            } else {
                if (rule.interval == 1) "Repeats monthly" else "Repeats every ${rule.interval} months"
            }
        }
        RecurrenceType.YEARLY -> {
            val monthName = try { Month.of(if (rule.yearlyMonth in 1..12) rule.yearlyMonth else 1).name.lowercase().replaceFirstChar { it.uppercase() } } catch (_: Exception) { "month" }
            if (rule.yearlyMode == YearlyMode.ORDINAL_WEEKDAY) {
                val ord = rule.ordinal?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "first"
                val day = rule.weekday?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Monday"
                if (rule.interval == 1) "Repeats yearly on the $ord $day of $monthName" else "Repeats every ${rule.interval} years on the $ord $day of $monthName"
            } else {
                if (rule.interval == 1) "Repeats yearly in $monthName" else "Repeats every ${rule.interval} years in $monthName"
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
    plans: List<Plan> = emptyList(),
    preselectedPlanId: String? = null,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        date: String,
        time: String,
        priority: String,
        notes: String,
        subTasksRaw: String,
        motivation: String,
        recurrenceType: String,
        recurrenceInterval: Int,
        planId: String?,
        notify7Days: Boolean,
        notify24Hours: Boolean,
        notifyDueNow: Boolean,
        recurrenceDaysOfWeek: String,
        recurrenceMonthlyMode: String,
        recurrenceYearlyMode: String,
        recurrenceOrdinal: String,
        recurrenceWeekday: String,
        recurrenceYearlyMonth: Int
    ) -> Unit
) {
    val now = remember { Calendar.getInstance() }
    val defaultDate = remember(now) { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now.time) }
    val defaultTime = remember(now) { SimpleDateFormat("HH:mm", Locale.US).format(now.time) }

    var taskName by remember { mutableStateOf(reminderToEdit?.title ?: "") }
    var selectedDate by remember { mutableStateOf(reminderToEdit?.date ?: defaultDate) }
    var selectedTime by remember { mutableStateOf(reminderToEdit?.time ?: defaultTime) }
    var priority by remember { mutableStateOf(reminderToEdit?.priority ?: Reminder.PRIORITY_IMPORTANT) }
    var notes by remember { mutableStateOf(reminderToEdit?.notes ?: "") }
    var selectedPlanId by remember { mutableStateOf(reminderToEdit?.planId ?: preselectedPlanId) }

    val initialRule = remember(reminderToEdit) {
        reminderToEdit?.toRecurrenceRule() ?: RecurrenceRule()
    }

    var recType by remember { mutableStateOf(initialRule.type) }
    var recInterval by remember { mutableStateOf(initialRule.interval) }
    var intervalInput by remember { mutableStateOf(recInterval.toString()) }
    var recDaysOfWeek by remember {
        mutableStateOf(
            if (initialRule.type == RecurrenceType.WEEKLY && initialRule.daysOfWeek.isEmpty()) {
                val inferredDay = try {
                    LocalDate.parse(selectedDate).dayOfWeek
                } catch (_: Exception) { null }
                if (inferredDay != null) setOf(RecurrenceDayOfWeek.valueOf(inferredDay.name))
                else setOf(RecurrenceDayOfWeek.MONDAY)
            } else {
                initialRule.daysOfWeek
            }
        )
    }
    var recMonthlyMode by remember { mutableStateOf(initialRule.monthlyMode) }
    var recYearlyMode by remember { mutableStateOf(initialRule.yearlyMode) }
    var recOrdinal by remember { mutableStateOf(initialRule.ordinal ?: RecurrenceOrdinal.FIRST) }
    var recWeekday by remember { mutableStateOf(initialRule.weekday ?: RecurrenceDayOfWeek.MONDAY) }
    var recYearlyMonth by remember {
        mutableStateOf(
            if (initialRule.yearlyMonth in 1..12) initialRule.yearlyMonth
            else (try { LocalDate.parse(selectedDate).monthValue } catch (_: Exception) { 1 })
        )
    }

    var ordinalDropdownExpanded by remember { mutableStateOf(false) }
    var weekdayDropdownExpanded by remember { mutableStateOf(false) }
    var yearlyMonthDropdownExpanded by remember { mutableStateOf(false) }

    val currentRecurrenceRule = remember(
        recType, recInterval, recDaysOfWeek, recMonthlyMode, recYearlyMode, recOrdinal, recWeekday, recYearlyMonth
    ) {
        RecurrenceRule(
            type = recType,
            interval = recInterval,
            daysOfWeek = recDaysOfWeek,
            monthlyMode = recMonthlyMode,
            yearlyMode = recYearlyMode,
            ordinal = recOrdinal,
            weekday = recWeekday,
            yearlyMonth = recYearlyMonth
        )
    }

    val isRecurrenceValid = remember(currentRecurrenceRule) {
        currentRecurrenceRule.validate() is RecurrenceRule.Result.Success
    }

    var notify7Days by remember { mutableStateOf(reminderToEdit?.notify7Days ?: false) }
    var notify24Hours by remember { mutableStateOf(reminderToEdit?.notify24Hours ?: false) }
    var notifyDueNow by remember { mutableStateOf(reminderToEdit?.notifyDueNow ?: true) }

    var subTasksList by remember {
        mutableStateOf(reminderToEdit?.subTasksRaw?.split("|")?.filter { it.isNotBlank() } ?: emptyList<String>())
    }
    var newSubTask by remember { mutableStateOf("") }

    var motivation by remember { mutableStateOf(reminderToEdit?.motivation ?: "") }
    var showAdvanced by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showPlanExplanation by remember { mutableStateOf(false) }
    var showUrgencyExplanation by remember { mutableStateOf(false) }
    var showRepeatExplanation by remember { mutableStateOf(false) }
    var planDropdownExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun onPipeAttempt() {
        Toast.makeText(context, "Tsk.R doesn't allow | here. I use that little line to keep your checklist organized.", Toast.LENGTH_SHORT).show()
    }

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
                    .imePadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 20.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(
                        text = if (reminderToEdit != null) "Edit Reminder" else "New Reminder",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Add the details and we'll help you stay on track.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // 1. Task Title Input
                    TextField(
                        value = taskName,
                        onValueChange = { taskName = it },
                        label = { Text("What needs to be done?") },
                        placeholder = { Text("Enter a task title...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    // 2. Plan (Optional) Section - Dropdown Menu
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Plan (Optional)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = { showPlanExplanation = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Plan Info",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (showPlanExplanation) {
                        AlertDialog(
                            onDismissRequest = { showPlanExplanation = false },
                            title = { Text("What is a Plan?", fontWeight = FontWeight.Bold) },
                            text = {
                                Text(
                                    text = "A Plan is a bigger goal or category that groups related tasks together (e.g. Work, Health, Moving).\n\nConnecting a task to a Plan helps you track overall progress towards that goal.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = { showPlanExplanation = false }) {
                                    Text("Got it")
                                }
                            }
                        )
                    }

                    val selectedPlan = remember(selectedPlanId, plans) { plans.find { it.id == selectedPlanId } }
                    ExposedDropdownMenuBox(
                        expanded = planDropdownExpanded,
                        onExpandedChange = { planDropdownExpanded = !planDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = selectedPlan?.name ?: "No Plan",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = null,
                                    tint = if (selectedPlan != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = planDropdownExpanded)
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = planDropdownExpanded,
                            onDismissRequest = { planDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("No Plan", fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(Icons.Default.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                                },
                                onClick = {
                                    selectedPlanId = null
                                    planDropdownExpanded = false
                                }
                            )
                            plans.forEach { plan ->
                                DropdownMenuItem(
                                    text = { Text(plan.name, fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    },
                                    onClick = {
                                        selectedPlanId = plan.id
                                        planDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 3. When Section
                    Text(
                        text = "When",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val formattedDateDisplay = remember(selectedDate) {
                        try {
                            val sdfSource = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            val date = sdfSource.parse(selectedDate)
                            if (date != null) {
                                val sdfDest = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                                sdfDest.format(date)
                            } else {
                                selectedDate
                            }
                        } catch (_: Exception) {
                            selectedDate
                        }
                    }

                    val formattedTimeDisplay = remember(selectedTime, context) {
                        try {
                            val parts = selectedTime.split(":")
                            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 12
                            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                            val is24Hour = DateFormat.is24HourFormat(context)
                            if (is24Hour) {
                                String.format(Locale.US, "%02d:%02d", hour, minute)
                            } else {
                                val amPm = if (hour >= 12) "PM" else "AM"
                                val displayHour = when {
                                    hour == 0 -> 12
                                    hour > 12 -> hour - 12
                                    else -> hour
                                }
                                String.format(Locale.US, "%d:%02d %s", displayHour, minute, amPm)
                            }
                        } catch (_: Exception) {
                            selectedTime
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                showDatePicker = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Select Date",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text("Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formattedDateDisplay, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }

                        Surface(
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                showTimePicker = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Select Time",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text("Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formattedTimeDisplay, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }

                    // 4. Urgency Section
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Urgency",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = { showUrgencyExplanation = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Urgency Info",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (showUrgencyExplanation) {
                        AlertDialog(
                            onDismissRequest = { showUrgencyExplanation = false },
                            title = { Text("Urgency Levels", fontWeight = FontWeight.Bold) },
                            text = {
                                Text(
                                    text = "• Important = Tsk.R gives this task stronger attention and higher visibility on your home screen and widget.\n\n• Normal = Standard reminder behavior.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = { showUrgencyExplanation = false }) {
                                    Text("Got it")
                                }
                            }
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val isImportantSelected = priority == Reminder.PRIORITY_IMPORTANT
                        Surface(
                            onClick = { priority = Reminder.PRIORITY_IMPORTANT },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isImportantSelected) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            contentColor = if (isImportantSelected) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            border = if (isImportantSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.error) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PriorityHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Important", fontWeight = FontWeight.Bold)
                            }
                        }

                        val isNormalSelected = priority == Reminder.PRIORITY_NORMAL
                        Surface(
                            onClick = { priority = Reminder.PRIORITY_NORMAL },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isNormalSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            contentColor = if (isNormalSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            border = if (isNormalSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Normal", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 5. Repeat / Recurrence 2.0 Section (Outlook-Inspired Material 3 Layout)
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Repeat",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = { showRepeatExplanation = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Repeat Info",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (showRepeatExplanation) {
                            AlertDialog(
                                onDismissRequest = { showRepeatExplanation = false },
                                title = { Text("Repeat vs Reminder Alerts", fontWeight = FontWeight.Bold) },
                                text = {
                                    Text(
                                        text = "Repeat controls when this task is scheduled again after completion.\n\nYour Reminder Alerts are separate and control when Tsk.R warns you before the task is due.",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = { showRepeatExplanation = false }) {
                                        Text("Got it")
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Top-Level Options: None, Daily, Weekly, Monthly, Yearly
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val options = listOf(
                                RecurrenceType.NONE to "None",
                                RecurrenceType.DAILY to "Daily",
                                RecurrenceType.WEEKLY to "Weekly",
                                RecurrenceType.MONTHLY to "Monthly",
                                RecurrenceType.YEARLY to "Yearly"
                            )
                            items(options) { (type, label) ->
                                FilterChip(
                                    selected = recType == type,
                                    onClick = {
                                        recType = type
                                        if (type == RecurrenceType.WEEKLY && recDaysOfWeek.isEmpty()) {
                                            val inferredDay = try {
                                                LocalDate.parse(selectedDate).dayOfWeek
                                            } catch (_: Exception) { null }
                                            if (inferredDay != null) {
                                                recDaysOfWeek = setOf(RecurrenceDayOfWeek.valueOf(inferredDay.name))
                                            } else {
                                                recDaysOfWeek = setOf(RecurrenceDayOfWeek.MONDAY)
                                            }
                                        }
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }

                        // Progressive Disclosure Panels
                        if (recType != RecurrenceType.NONE) {
                            Spacer(modifier = Modifier.height(12.dp))

                            // Interval Controls
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Every", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                IconButton(
                                    onClick = {
                                        if (recInterval > 1) {
                                            recInterval--
                                            intervalInput = recInterval.toString()
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease interval")
                                }
                                TextField(
                                    value = intervalInput,
                                    onValueChange = { input ->
                                        intervalInput = input
                                        val parsed = input.toIntOrNull()
                                        if (parsed != null && parsed > 0) {
                                            recInterval = parsed
                                        }
                                    },
                                    modifier = Modifier.width(64.dp),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    )
                                )
                                IconButton(
                                    onClick = {
                                        recInterval++
                                        intervalInput = recInterval.toString()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase interval")
                                }

                                val unitLabel = when (recType) {
                                    RecurrenceType.DAILY -> if (recInterval == 1) "day" else "days"
                                    RecurrenceType.WEEKLY -> if (recInterval == 1) "week" else "weeks"
                                    RecurrenceType.MONTHLY -> if (recInterval == 1) "month" else "months"
                                    RecurrenceType.YEARLY -> if (recInterval == 1) "year" else "years"
                                    else -> ""
                                }
                                Text(unitLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }

                            // WEEKLY Panel
                            if (recType == RecurrenceType.WEEKLY) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("On days:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val days = listOf(
                                        RecurrenceDayOfWeek.MONDAY to "M",
                                        RecurrenceDayOfWeek.TUESDAY to "T",
                                        RecurrenceDayOfWeek.WEDNESDAY to "W",
                                        RecurrenceDayOfWeek.THURSDAY to "T",
                                        RecurrenceDayOfWeek.FRIDAY to "F",
                                        RecurrenceDayOfWeek.SATURDAY to "S",
                                        RecurrenceDayOfWeek.SUNDAY to "S"
                                    )
                                    for ((dayEnum, label) in days) {
                                        val isSelected = recDaysOfWeek.contains(dayEnum)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                recDaysOfWeek = if (isSelected) {
                                                    if (recDaysOfWeek.size > 1) recDaysOfWeek - dayEnum else recDaysOfWeek
                                                } else {
                                                    recDaysOfWeek + dayEnum
                                                }
                                            },
                                            label = { Text(label, fontWeight = FontWeight.Bold) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            // MONTHLY Panel
                            if (recType == RecurrenceType.MONTHLY) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = recMonthlyMode == MonthlyMode.DAY_OF_MONTH,
                                        onClick = { recMonthlyMode = MonthlyMode.DAY_OF_MONTH },
                                        label = { Text("Day") }
                                    )
                                    FilterChip(
                                        selected = recMonthlyMode == MonthlyMode.ORDINAL_WEEKDAY,
                                        onClick = { recMonthlyMode = MonthlyMode.ORDINAL_WEEKDAY },
                                        label = { Text("Weekday") }
                                    )
                                }

                                if (recMonthlyMode == MonthlyMode.ORDINAL_WEEKDAY) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("On the", style = MaterialTheme.typography.bodyMedium)

                                        AssistChip(
                                            onClick = { ordinalDropdownExpanded = true },
                                            label = { Text(recOrdinal.name.lowercase().replaceFirstChar { it.uppercase() }) }
                                        )
                                        DropdownMenu(
                                            expanded = ordinalDropdownExpanded,
                                            onDismissRequest = { ordinalDropdownExpanded = false }
                                        ) {
                                            for (ord in RecurrenceOrdinal.entries) {
                                                DropdownMenuItem(
                                                    text = { Text(ord.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                                    onClick = {
                                                        recOrdinal = ord
                                                        ordinalDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }

                                        AssistChip(
                                            onClick = { weekdayDropdownExpanded = true },
                                            label = { Text(recWeekday.name.lowercase().replaceFirstChar { it.uppercase() }) }
                                        )
                                        DropdownMenu(
                                            expanded = weekdayDropdownExpanded,
                                            onDismissRequest = { weekdayDropdownExpanded = false }
                                        ) {
                                            for (day in RecurrenceDayOfWeek.entries) {
                                                DropdownMenuItem(
                                                    text = { Text(day.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                                    onClick = {
                                                        recWeekday = day
                                                        weekdayDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // YEARLY Panel
                            if (recType == RecurrenceType.YEARLY) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = recYearlyMode == YearlyMode.SPECIFIC_DATE,
                                        onClick = { recYearlyMode = YearlyMode.SPECIFIC_DATE },
                                        label = { Text("Date") }
                                    )
                                    FilterChip(
                                        selected = recYearlyMode == YearlyMode.ORDINAL_WEEKDAY,
                                        onClick = { recYearlyMode = YearlyMode.ORDINAL_WEEKDAY },
                                        label = { Text("Weekday") }
                                    )
                                }

                                if (recYearlyMode == YearlyMode.ORDINAL_WEEKDAY) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("On the", style = MaterialTheme.typography.bodyMedium)

                                        AssistChip(
                                            onClick = { ordinalDropdownExpanded = true },
                                            label = { Text(recOrdinal.name.lowercase().replaceFirstChar { it.uppercase() }) }
                                        )
                                        DropdownMenu(
                                            expanded = ordinalDropdownExpanded,
                                            onDismissRequest = { ordinalDropdownExpanded = false }
                                        ) {
                                            for (ord in RecurrenceOrdinal.entries) {
                                                DropdownMenuItem(
                                                    text = { Text(ord.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                                    onClick = {
                                                        recOrdinal = ord
                                                        ordinalDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }

                                        AssistChip(
                                            onClick = { weekdayDropdownExpanded = true },
                                            label = { Text(recWeekday.name.lowercase().replaceFirstChar { it.uppercase() }) }
                                        )
                                        DropdownMenu(
                                            expanded = weekdayDropdownExpanded,
                                            onDismissRequest = { weekdayDropdownExpanded = false }
                                        ) {
                                            for (day in RecurrenceDayOfWeek.entries) {
                                                DropdownMenuItem(
                                                    text = { Text(day.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                                    onClick = {
                                                        recWeekday = day
                                                        weekdayDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }

                                        Text("of", style = MaterialTheme.typography.bodyMedium)

                                        val monthName = try {
                                            Month.of(if (recYearlyMonth in 1..12) recYearlyMonth else 1).name.lowercase().replaceFirstChar { it.uppercase() }
                                        } catch (_: Exception) { "January" }

                                        AssistChip(
                                            onClick = { yearlyMonthDropdownExpanded = true },
                                            label = { Text(monthName) }
                                        )
                                        DropdownMenu(
                                            expanded = yearlyMonthDropdownExpanded,
                                            onDismissRequest = { yearlyMonthDropdownExpanded = false }
                                        ) {
                                            for (m in 1..12) {
                                                val name = Month.of(m).name.lowercase().replaceFirstChar { it.uppercase() }
                                                DropdownMenuItem(
                                                    text = { Text(name) },
                                                    onClick = {
                                                        recYearlyMonth = m
                                                        yearlyMonthDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Summary Banner
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Repeat,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = buildRecurrenceSummary(currentRecurrenceRule),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // 6. Advanced Settings Collapsible Section
                    Column {
                        TextButton(
                            onClick = { showAdvanced = !showAdvanced },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Advanced Settings (Optional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }

                        if (showAdvanced) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Early Warning Alerts
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Early Warning Alerts",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Tsk.R will always trigger Mission Mode at the scheduled time.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("24 hours before", style = MaterialTheme.typography.bodyMedium)
                                        Switch(checked = notify24Hours, onCheckedChange = { notify24Hours = it })
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("7 days before", style = MaterialTheme.typography.bodyMedium)
                                        Switch(checked = notify7Days, onCheckedChange = { notify7Days = it })
                                    }
                                }

                                // Notes
                                TextField(
                                    value = notes,
                                    onValueChange = { notes = it },
                                    label = { Text("Notes (Optional)") },
                                    placeholder = { Text("Add any strategic notes here...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 4,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    )
                                )

                                // Checklist
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Steps / Task Breakdown",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    subTasksList.forEachIndexed { index, step ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "• $step",
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(onClick = {
                                                subTasksList = subTasksList.toMutableList().apply { removeAt(index) }
                                            }) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove step", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        TextField(
                                            value = newSubTask,
                                            onValueChange = { input ->
                                                if (input.contains("|")) {
                                                    onPipeAttempt()
                                                } else {
                                                    newSubTask = input
                                                }
                                            },
                                            label = { Text("Add a step") },
                                            placeholder = { Text("e.g. Buy ingredients") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(16.dp),
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent
                                            ),
                                            keyboardActions = KeyboardActions(
                                                onDone = {
                                                    if (newSubTask.isNotBlank() && !newSubTask.contains("|")) {
                                                        subTasksList = subTasksList + newSubTask.trim()
                                                        newSubTask = ""
                                                    }
                                                }
                                            )
                                        )
                                        IconButton(
                                            onClick = {
                                                if (newSubTask.isNotBlank() && !newSubTask.contains("|")) {
                                                    subTasksList = subTasksList + newSubTask.trim()
                                                    newSubTask = ""
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Add step", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }

                                // Custom Mission Quote
                                TextField(
                                    value = motivation,
                                    onValueChange = { motivation = it },
                                    label = { Text("Custom Mission Quote (Optional)") },
                                    placeholder = { Text("e.g. Future-you is watching!") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 2,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }

                } // End of scrollable column

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            if (taskName.isNotBlank() && isRecurrenceValid) {
                                val combinedSubTasks = subTasksList.joinToString("|")
                                val validInterval = if (recInterval > 0) recInterval else 1
                                onSave(
                                    taskName,
                                    selectedDate,
                                    selectedTime,
                                    priority,
                                    notes,
                                    combinedSubTasks,
                                    motivation,
                                    currentRecurrenceRule.type.name,
                                    validInterval,
                                    selectedPlanId,
                                    notify7Days,
                                    notify24Hours,
                                    notifyDueNow,
                                    currentRecurrenceRule.getSerializedDaysOfWeek(),
                                    currentRecurrenceRule.monthlyMode.name,
                                    currentRecurrenceRule.yearlyMode.name,
                                    currentRecurrenceRule.ordinal?.name ?: "",
                                    currentRecurrenceRule.weekday?.name ?: "",
                                    currentRecurrenceRule.yearlyMonth
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = taskName.isNotBlank() && isRecurrenceValid,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (reminderToEdit != null) "Save Changes" else "Schedule", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (showDatePicker) {
            M3ModalDatePicker(
                initialDate = selectedDate,
                onDateSelected = { selectedDate = it; showDatePicker = false },
                onDismiss = { showDatePicker = false }
            )
        }

        if (showTimePicker) {
            M3ModalTimePicker(
                initialTime = selectedTime,
                onTimeSelected = { selectedTime = it; showTimePicker = false },
                onDismiss = { showTimePicker = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlanBottomSheet(
    planToEdit: Plan? = null,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        description: String,
        icon: String,
        deadline: String?,
        category: String,
        categorySource: String,
        color: String,
        colorSource: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(planToEdit?.name ?: "") }
    var description by remember { mutableStateOf(planToEdit?.description ?: "") }
    var deadline by remember { mutableStateOf(planToEdit?.deadline ?: "") }

    var category by remember { mutableStateOf(planToEdit?.getPlanCategory() ?: PlanCategory.PERSONAL) }
    var categorySource by remember {
        mutableStateOf(
            if (planToEdit != null) planToEdit.getCategorySource() else CategorySource.AUTOMATIC
        )
    }
    var color by remember { mutableStateOf(planToEdit?.getPlanColor() ?: PlanCategoryColorMapping.getDefaultColor(category)) }
    var colorSource by remember {
        mutableStateOf(
            if (planToEdit != null) planToEdit.getColorSource() else ColorSource.CATEGORY
        )
    }

    var showCategoryPicker by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()

    // Automatic classification on name change if categorySource == AUTOMATIC
    LaunchedEffect(name) {
        if (categorySource == CategorySource.AUTOMATIC && name.isNotBlank()) {
            val suggested = PlanCategoryClassifier.classify(name)
            category = suggested
            if (colorSource == ColorSource.CATEGORY) {
                color = PlanCategoryColorMapping.getDefaultColor(suggested)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 20.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (planToEdit != null) "Edit Plan" else "New Plan",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("What are you trying to accomplish?") },
                placeholder = { Text("e.g. Launch My App") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            // Category & Color Identity Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Chip
                OutlinedButton(
                    onClick = { showCategoryPicker = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    val iconVector = TskRPlanIcons.getIcon(category)
                    Icon(imageVector = iconVector, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(category.displayName, style = MaterialTheme.typography.labelMedium)
                }

                // Color Chip
                val effectiveColor = if (colorSource == ColorSource.CATEGORY) {
                    PlanCategoryColorMapping.getDefaultColor(category)
                } else {
                    color
                }
                val resolvedColor = PlanColorResolver.resolve(effectiveColor, isDark)

                OutlinedButton(
                    onClick = { showColorPicker = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(resolvedColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(effectiveColor.displayName, style = MaterialTheme.typography.labelMedium)
                }
            }

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (deadline.isBlank()) "Set Optional Deadline" else "Deadline: $deadline", style = MaterialTheme.typography.labelMedium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel", color = MaterialTheme.colorScheme.outline)
                }

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val finalColor = if (colorSource == ColorSource.CATEGORY) {
                                PlanCategoryColorMapping.getDefaultColor(category)
                            } else {
                                color
                            }
                            onSave(
                                name,
                                description,
                                category.name,
                                if (deadline.isBlank()) null else deadline,
                                category.name,
                                categorySource.name,
                                finalColor.name,
                                colorSource.name
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (planToEdit != null) "Save Changes" else "Create Plan", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Category Picker Dialog
        if (showCategoryPicker) {
            AlertDialog(
                onDismissRequest = { showCategoryPicker = false },
                title = { Text("Select Category") },
                text = {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(PlanCategory.entries) { cat ->
                            val isSelected = category == cat
                            Surface(
                                onClick = {
                                    category = cat
                                    categorySource = CategorySource.MANUAL
                                    if (colorSource == ColorSource.CATEGORY) {
                                        color = PlanCategoryColorMapping.getDefaultColor(cat)
                                    }
                                    showCategoryPicker = false
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = TskRPlanIcons.getIcon(cat),
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = cat.displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCategoryPicker = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Color Picker Dialog
        if (showColorPicker) {
            AlertDialog(
                onDismissRequest = { showColorPicker = false },
                title = { Text("Select Plan Color") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                colorSource = ColorSource.CATEGORY
                                color = PlanCategoryColorMapping.getDefaultColor(category)
                                showColorPicker = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Use Category Color (${PlanCategoryColorMapping.getDefaultColor(category).displayName})")
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.heightIn(max = 240.dp)
                        ) {
                            items(PlanColor.entries) { col ->
                                val isSelected = colorSource == ColorSource.MANUAL && color == col
                                val swatchColor = PlanColorResolver.resolve(col, isDark)
                                Surface(
                                    onClick = {
                                        color = col
                                        colorSource = ColorSource.MANUAL
                                        showColorPicker = false
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(swatchColor)
                                        )
                                        Text(
                                            text = col.displayName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showColorPicker = false }) {
                        Text("Close")
                    }
                }
            )
        }

        if (showDatePicker) {
            M3ModalDatePicker(
                initialDate = if (deadline.isBlank()) SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) else deadline,
                onDateSelected = { deadline = it; showDatePicker = false },
                onDismiss = { showDatePicker = false }
            )
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
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sdf.parse(initialDate)?.time
        } catch (_: Exception) {
            null
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val selectedMillis = datePickerState.selectedDateMillis
                if (selectedMillis != null) {
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                        timeInMillis = selectedMillis
                    }
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    onDateSelected(sdf.format(cal.time))
                }
            }) {
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
fun M3ModalTimePicker(
    initialTime: String,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val parts = initialTime.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 12
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val formattedTime = String.format(Locale.US, "%02d:%02d", timePickerState.hour, timePickerState.minute)
                onTimeSelected(formattedTime)
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}

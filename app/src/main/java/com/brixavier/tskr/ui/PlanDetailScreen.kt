package com.brixavier.tskr.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brixavier.tskr.bloc.ReminderEvent
import com.brixavier.tskr.bloc.ReminderState
import com.brixavier.tskr.model.Plan
import com.brixavier.tskr.model.PlanProgress
import com.brixavier.tskr.personality.ActionType
import com.brixavier.tskr.personality.PersonalityContext
import com.brixavier.tskr.personality.PersonalityEngine
import com.brixavier.tskr.personality.PlanStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailScreen(
    planId: String,
    state: ReminderState,
    personalityEngine: PersonalityEngine,
    onEvent: (ReminderEvent) -> Unit,
    onEditPlan: (Plan) -> Unit,
    onCreateTaskForPlan: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val plan = state.plans.find { it.id == planId }
    val progress = state.planProgress.find { it.planId == planId } ?: PlanProgress(planId, 0, 0)
    val associatedTasks = state.reminders.filter { it.planId == planId }

    if (plan == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Plan not found")
        }
        return
    }

    val planContext = remember(plan.id, plan.status, progress.completedTasks, progress.totalTasks, plan.deadline) {
        val daysUntilDeadline = plan.deadline?.let { deadlineStr ->
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val target = sdf.parse(deadlineStr)
                if (target != null) {
                    val diffMillis = target.time - System.currentTimeMillis()
                    (diffMillis / (1000 * 60 * 60 * 24)).toInt()
                } else null
            } catch (_: Exception) {
                null
            }
        }
        val statusEnum = when (plan.status) {
            Plan.STATUS_COMPLETED -> PlanStatus.COMPLETED
            Plan.STATUS_ARCHIVED -> PlanStatus.ARCHIVED
            else -> PlanStatus.ACTIVE
        }
        val actionType = when {
            plan.status == Plan.STATUS_COMPLETED -> ActionType.PLAN_COMPLETED
            plan.status == Plan.STATUS_ARCHIVED -> ActionType.PLAN_ARCHIVED
            progress.totalTasks == 0 -> ActionType.PLAN_NO_TASKS
            else -> ActionType.PLAN_PROGRESS_MILESTONE
        }
        PersonalityContext(
            actionType = actionType,
            planProgressRatio = progress.progressRatio,
            planTotalTasks = progress.totalTasks,
            planCompletedTasks = progress.completedTasks,
            planDaysUntilDeadline = daysUntilDeadline,
            planStatus = statusEnum,
            isFirstTaskCompleted = progress.completedTasks == 1 && progress.totalTasks > 0
        )
    }

    val planPersonality = remember(planContext) {
        personalityEngine.getMessage(planContext).text
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (plan.icon.isNotBlank()) {
                            Text(text = plan.icon, fontSize = 22.sp)
                        }
                        Text(plan.name, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEditPlan(plan) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Plan")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Plan", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onCreateTaskForPlan(plan.id) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task to Plan")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Plan Personality Quote Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("💡", fontSize = 20.sp)
                    Text(
                        text = planPersonality,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // Plan Metadata Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (plan.description.isNotBlank()) {
                        Text(text = plan.description, style = MaterialTheme.typography.bodyLarge)
                    }
                    if (!plan.deadline.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text(text = "Deadline: ${plan.deadline}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                    }
                    
                    // Progress Bar
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Progress", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (progress.totalTasks == 0) "No tasks yet" else "${progress.completedTasks} / ${progress.totalTasks} completed (${(progress.progressRatio * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (progress.totalTasks > 0) {
                            LinearProgressIndicator(
                                progress = { progress.progressRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }

            // Plan Status Manual Control Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Plan Status", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Plan.STATUS_ACTIVE to "Active",
                            Plan.STATUS_COMPLETED to "Completed",
                            Plan.STATUS_ARCHIVED to "Archived"
                        ).forEach { (statusVal, label) ->
                            FilterChip(
                                selected = plan.status == statusVal,
                                onClick = {
                                    val updatedPlan = plan.copy(status = statusVal)
                                    onEvent(ReminderEvent.UpdatePlan(updatedPlan))
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            Text(
                text = "Connected Tasks",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (associatedTasks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No tasks yet. This goal is waiting for your action.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    items(associatedTasks, key = { it.id }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onClick = {
                                // Edit task
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

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Plan?") },
            text = { Text("Deleting this plan will detach its tasks, but your tasks will not be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    onEvent(ReminderEvent.DeletePlan(plan.id))
                    showDeleteDialog = false
                    onBack()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

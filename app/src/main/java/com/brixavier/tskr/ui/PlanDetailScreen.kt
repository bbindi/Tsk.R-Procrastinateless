package com.brixavier.tskr.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brixavier.tskr.bloc.ReminderEvent
import com.brixavier.tskr.bloc.ReminderState
import com.brixavier.tskr.model.Plan
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.model.document.DocumentBlock
import com.brixavier.tskr.model.document.PlanDocument
import com.brixavier.tskr.model.document.PlanDocumentEditor
import com.brixavier.tskr.model.document.PlanDocumentSerializer
import com.brixavier.tskr.model.document.TextRun
import com.brixavier.tskr.personality.PersonalityEngine
import com.brixavier.tskr.ui.TskRPlanIcons
import com.brixavier.tskr.ui.PlanColorResolver
import com.brixavier.tskr.ui.richeditor.RichEditorPrototype
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import com.brixavier.tskr.ui.theme.TskRMotion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailScreen(
    planId: String,
    state: ReminderState,
    onEvent: (ReminderEvent) -> Unit,
    onEditPlan: (Plan) -> Unit,
    onCreateTaskForPlan: (String) -> Unit,
    onReminderClick: (Reminder) -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val scrollState = rememberScrollState()
    val plan = state.plans.find { it.id == planId }
    val associatedTasks = state.reminders.filter { it.planId == planId }
    val totalConnectedTasks = associatedTasks.size
    val completedConnectedTasks = remember(associatedTasks) { associatedTasks.count { !it.isActive } }
    val connectedTaskProgressRatio = if (totalConnectedTasks > 0) completedConnectedTasks.toFloat() / totalConnectedTasks.toFloat() else 0f

    val sections = remember(planId, state.noteSections) {
        state.noteSections.filter { it.planId == planId }.sortedBy { it.sortOrder }
    }

    val steps = remember(planId, state.planSteps) {
        state.planSteps.filter { it.planId == planId }.sortedBy { it.sortOrder }
    }
    val completedStepCount = remember(steps) { steps.count { it.isCompleted } }
    var newStepText by remember { mutableStateOf("") }

    var isStepsExpanded by remember { mutableStateOf(steps.isNotEmpty()) }
    var isTasksExpanded by remember { mutableStateOf(associatedTasks.isNotEmpty()) }

    // Bootstrap default "Notes" section for newly created or empty plans
    LaunchedEffect(planId, sections.isEmpty()) {
        if (plan != null && sections.isEmpty()) {
            onEvent(ReminderEvent.AddNoteSection(planId = planId, title = "Notes", content = ""))
        }
    }

    var selectedSectionId by remember { mutableStateOf("") }
    var prevSectionIds by remember { mutableStateOf(emptySet<String>()) }

    LaunchedEffect(sections) {
        val currentIds = sections.map { it.id }.toSet()
        val newIds = currentIds - prevSectionIds
        if (prevSectionIds.isNotEmpty() && newIds.isNotEmpty()) {
            selectedSectionId = newIds.first()
        } else if (selectedSectionId.isEmpty() || sections.none { it.id == selectedSectionId }) {
            selectedSectionId = sections.firstOrNull()?.id ?: ""
        }
        prevSectionIds = currentIds
    }

    val activeSection = remember(sections, selectedSectionId) {
        sections.find { it.id == selectedSectionId } ?: sections.firstOrNull()
    }

    val parsedDoc = remember(activeSection?.id, activeSection?.content) {
        PlanNoteEditorAdapter.loadDocument(activeSection)
    }

    var showAddSectionDialog by remember { mutableStateOf(false) }
    var showRenameSectionDialog by remember { mutableStateOf(false) }
    var showDeleteSectionDialog by remember { mutableStateOf(false) }
    var showDeletePlanDialog by remember { mutableStateOf(false) }

    var newSectionTitle by remember { mutableStateOf("") }
    var renameSectionTitle by remember { mutableStateOf("") }

    if (plan == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Plan not found")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val planIcon = TskRPlanIcons.getIcon(plan.getPlanCategory())
                        val rawColor = plan.resolveEffectiveColor()
                        val resolvedColor = PlanColorResolver.resolve(rawColor, isSystemInDarkTheme())
                        val animatedPlanAccentColor by animateColorAsState(
                            targetValue = resolvedColor,
                            animationSpec = tween(durationMillis = TskRMotion.DURATION_STANDARD, easing = TskRMotion.EasingStandard),
                            label = "PlanDetailAccentColorAnimation"
                        )
                        Icon(
                            imageVector = planIcon,
                            contentDescription = plan.getPlanCategory().displayName,
                            tint = animatedPlanAccentColor,
                            modifier = Modifier.size(24.dp)
                        )
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
                    IconButton(onClick = { showDeletePlanDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Plan", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Plan Goal / Overview Header Snippet
            if (plan.description.isNotBlank()) {
                Text(
                    text = plan.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }

            // Collapsible Plan Steps Checklist Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isStepsExpanded = !isStepsExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checklist,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Plan Steps",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (steps.isEmpty()) "0 steps" else "$completedStepCount of ${steps.size} completed",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (isStepsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Steps",
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    if (isStepsExpanded) {
                        if (steps.isNotEmpty()) {
                            val targetStepProgress = completedStepCount.toFloat() / steps.size.toFloat()
                            val animatedStepProgress by animateFloatAsState(
                                targetValue = targetStepProgress,
                                animationSpec = tween(durationMillis = TskRMotion.DURATION_STANDARD, easing = TskRMotion.EasingStandard),
                                label = "PlanStepProgressAnimation"
                            )
                            LinearProgressIndicator(
                                progress = { animatedStepProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = MaterialTheme.colorScheme.tertiary,
                                trackColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                                strokeCap = StrokeCap.Round
                            )
                        }

                        steps.forEach { step ->
                            key(step.id) {
                                val stepCompleted = step.isCompleted
                                val animatedStepAlpha by animateFloatAsState(
                                    targetValue = if (stepCompleted) 0.6f else 1.0f,
                                    animationSpec = tween(durationMillis = TskRMotion.DURATION_MICRO, easing = TskRMotion.EasingStandard),
                                    label = "PlanStepAlphaAnimation"
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer { alpha = animatedStepAlpha },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
                                        checked = step.isCompleted,
                                        onCheckedChange = { checked ->
                                            onEvent(ReminderEvent.UpdatePlanStep(step.copy(isCompleted = checked)))
                                        }
                                    )
                                    Text(
                                        text = step.text,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            textDecoration = if (step.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        color = if (step.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { onEvent(ReminderEvent.DeletePlanStep(step)) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove step",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Add Step Input Field
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newStepText,
                                onValueChange = { newStepText = it },
                                placeholder = { Text("Add a lightweight plan step...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (newStepText.isNotBlank()) {
                                            onEvent(ReminderEvent.AddPlanStep(planId = plan.id, text = newStepText.trim()))
                                            newStepText = ""
                                        }
                                    }
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            IconButton(
                                onClick = {
                                    if (newStepText.isNotBlank()) {
                                        onEvent(ReminderEvent.AddPlanStep(planId = plan.id, text = newStepText.trim()))
                                        newStepText = ""
                                    }
                                },
                                enabled = newStepText.isNotBlank()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add step",
                                    tint = if (newStepText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }

            // Collapsible Connected Tasks Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isTasksExpanded = !isTasksExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Connected Tasks",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (totalConnectedTasks == 0) "No tasks yet" else "$completedConnectedTasks of $totalConnectedTasks completed",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (isTasksExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Tasks",
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    if (isTasksExpanded) {
                        if (totalConnectedTasks > 0) {
                            val animatedConnectedTaskProgress by animateFloatAsState(
                                targetValue = connectedTaskProgressRatio,
                                animationSpec = tween(durationMillis = TskRMotion.DURATION_STANDARD, easing = TskRMotion.EasingStandard),
                                label = "ConnectedTaskProgressAnimation"
                            )
                            LinearProgressIndicator(
                                progress = { animatedConnectedTaskProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                strokeCap = StrokeCap.Round
                            )
                        }

                        if (associatedTasks.isEmpty()) {
                            Text(
                                text = "No tasks assigned to this plan yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            associatedTasks.forEach { reminder ->
                                ReminderCard(
                                    reminder = reminder,
                                    onClick = {
                                        onReminderClick(reminder)
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

                        TextButton(
                            onClick = { onCreateTaskForPlan(plan.id) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.tertiary
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Connected Task", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Notes & Document Workspace
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notes & Document Workspace",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (activeSection != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = {
                                    renameSectionTitle = activeSection.title
                                    showRenameSectionDialog = true
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename Section", modifier = Modifier.size(16.dp))
                            }
                            if (sections.size > 1) {
                                IconButton(
                                    onClick = { showDeleteSectionDialog = true },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Section", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(sections, key = { it.id }) { sec ->
                        val isSelected = activeSection?.id == sec.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedSectionId = sec.id
                            },
                            label = { Text(sec.title, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }

                    item {
                        IconButton(
                            onClick = {
                                newSectionTitle = ""
                                showAddSectionDialog = true
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Section", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }

            // Rich Text Editor
            if (activeSection != null) {
                key(activeSection.id) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 150.dp)
                    ) {
                        RichEditorPrototype(
                            sectionId = activeSection.id,
                            document = parsedDoc,
                            onDocumentChanged = { newDoc ->
                                val serialized = PlanDocumentSerializer.serialize(newDoc)
                                if (activeSection.content != serialized) {
                                    onEvent(ReminderEvent.UpdateNoteSection(activeSection.copy(content = serialized)))
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Add Section Dialog
    if (showAddSectionDialog) {
        AlertDialog(
            onDismissRequest = { showAddSectionDialog = false },
            title = { Text("Add Note Section") },
            text = {
                OutlinedTextField(
                    value = newSectionTitle,
                    onValueChange = { newSectionTitle = it },
                    label = { Text("Section Title") },
                    placeholder = { Text("e.g. Discussion, Decisions, Ideas") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSectionTitle.isNotBlank()) {
                            onEvent(ReminderEvent.AddNoteSection(planId = plan.id, title = newSectionTitle.trim()))
                            newSectionTitle = ""
                            showAddSectionDialog = false
                        }
                    },
                    enabled = newSectionTitle.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSectionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Section Dialog
    if (showRenameSectionDialog && activeSection != null) {
        AlertDialog(
            onDismissRequest = { showRenameSectionDialog = false },
            title = { Text("Rename Section") },
            text = {
                OutlinedTextField(
                    value = renameSectionTitle,
                    onValueChange = { renameSectionTitle = it },
                    label = { Text("Section Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameSectionTitle.isNotBlank()) {
                            onEvent(ReminderEvent.UpdateNoteSection(activeSection.copy(title = renameSectionTitle.trim())))
                            showRenameSectionDialog = false
                        }
                    },
                    enabled = renameSectionTitle.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameSectionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Section Dialog
    if (showDeleteSectionDialog && activeSection != null) {
        AlertDialog(
            onDismissRequest = { showDeleteSectionDialog = false },
            title = { Text("Delete Section?") },
            text = { Text("Are you sure you want to delete section \"${activeSection.title}\"? Notes in this section will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    onEvent(ReminderEvent.DeleteNoteSection(activeSection))
                    showDeleteSectionDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSectionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Plan Dialog
    if (showDeletePlanDialog) {
        AlertDialog(
            onDismissRequest = { showDeletePlanDialog = false },
            title = { Text("Delete Plan?") },
            text = { Text("Deleting this plan will remove its notes and detach its tasks, but your tasks will not be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    onEvent(ReminderEvent.DeletePlan(plan.id))
                    showDeletePlanDialog = false
                    onBack()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePlanDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

package com.brixavier.tskr.ui

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brixavier.tskr.bloc.ReminderEvent
import com.brixavier.tskr.model.Reminder

private val motivationQuotes = listOf(
    "Why are you staring at notes? The clock is literally ticking!",
    "Reading this won't get the task done. Get to work!",
    "Your future self is currently groaning at your procrastination.",
    "Action is the antidote to anxiety. Start now.",
    "This mission is critical. Your potential is higher than your excuses."
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDetailScreen(
    reminder: Reminder,
    onEvent: (ReminderEvent) -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val randomMotivation = remember { motivationQuotes.random() }

    val priorityColor = if (reminder.isImportant()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mission Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                .padding(24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header with Priority
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(priorityColor, CircleShape)
                )
                Text(
                    text = reminder.getPriorityDisplayName(),
                    style = MaterialTheme.typography.labelLarge,
                    color = priorityColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Text(
                text = reminder.title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Timeline Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = reminder.date, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = reminder.time, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                    
                    if (reminder.isActive) {
                        Divider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            text = getRelativeTimeSpan(reminder.date, reminder.time),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Notes Section
            if (reminder.notes.isNotBlank()) {
                Column {
                    Text(text = "Strategic Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = reminder.notes,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Checklist
            if (reminder.subTasksRaw.isNotBlank()) {
                Column {
                    Text(text = "Task Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val steps = reminder.subTasksRaw.split("|")
                    steps.forEachIndexed { index, step ->
                        val trimmed = step.trim()
                        val isCompleted = trimmed.startsWith("[x] ")
                        val text = if (isCompleted) trimmed.substring(4) else if (trimmed.startsWith("[ ] ")) trimmed.substring(4) else trimmed
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Checkbox(
                                checked = isCompleted,
                                onCheckedChange = { checked ->
                                    val updatedSteps = steps.toMutableList()
                                    updatedSteps[index] = if (checked) "[x] $text" else "[ ] $text"
                                    val newRaw = updatedSteps.joinToString("|")
                                    onEvent(ReminderEvent.UpdateReminder(reminder.copy(subTasksRaw = newRaw)))
                                }
                            )
                            Text(text = text)
                        }
                    }
                }
            }

            // Motivation Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = randomMotivation,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { 
                    onEvent(ReminderEvent.ToggleReminder(reminder.id, !reminder.isActive))
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (reminder.isActive) "MARK AS COMPLETED" else "RESTORE TASK")
            }
        }
    }
}

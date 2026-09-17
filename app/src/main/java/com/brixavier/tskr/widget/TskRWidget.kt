package com.brixavier.tskr.widget

import android.R as AndroidR
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.brixavier.tskr.MainActivity
import com.brixavier.tskr.data.ReminderDatabase
import com.brixavier.tskr.ui.getRelativeTimeSpan

class TskRWidget : GlanceAppWidget() {

    companion object {
        val SMALL_SQUARE = DpSize(120.dp, 120.dp)
        val MEDIUM_RECTANGLE = DpSize(220.dp, 120.dp)
        val LARGE_RECTANGLE = DpSize(260.dp, 200.dp)

        val OpenAddSheetKey = ActionParameters.Key<Boolean>(MainActivity.EXTRA_OPEN_ADD_SHEET)
        val ReminderIdKey = ActionParameters.Key<String>(MainActivity.EXTRA_OPEN_REMINDER_ID)
        val OccurrenceIdKey = ActionParameters.Key<String>("EXTRA_OCCURRENCE_ID")
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(SMALL_SQUARE, MEDIUM_RECTANGLE, LARGE_RECTANGLE)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = ReminderDatabase.getDatabase(context)
        val widgetState = WidgetDataResolver.resolveWidgetState(database, limit = 5)

        provideContent {
            GlanceTheme {
                TskRWidgetContent(widgetState)
            }
        }
    }

    @Composable
    private fun TskRWidgetContent(state: WidgetUiState) {
        val size = LocalSize.current

        when {
            size.height >= LARGE_RECTANGLE.height -> LargeWidgetLayout(state)
            size.width >= MEDIUM_RECTANGLE.width -> MediumWidgetLayout(state)
            else -> SmallWidgetLayout(state)
        }
    }

    /**
     * Small 2x2 widget layout focusing on quick accountability glance.
     */
    @Composable
    private fun SmallWidgetLayout(state: WidgetUiState) {
        val tasks = state.pendingTasks

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.background)
                .padding(10.dp),
            verticalAlignment = Alignment.Vertical.Top
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = "Tsk.R",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                Box(
                    modifier = GlanceModifier
                        .background(GlanceTheme.colors.primaryContainer)
                        .cornerRadius(12.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clickable(actionStartActivity<MainActivity>(actionParametersOf(OpenAddSheetKey to true))),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(AndroidR.drawable.ic_input_add),
                        contentDescription = "Add Task",
                        modifier = GlanceModifier.size(14.dp)
                    )
                }
            }

            if (tasks.isNotEmpty()) {
                val primaryTask = tasks[0]
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surfaceVariant)
                        .cornerRadius(14.dp)
                        .padding(10.dp)
                ) {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .clickable(actionStartActivity<MainActivity>(
                                actionParametersOf(
                                    ReminderIdKey to primaryTask.reminderId,
                                    OccurrenceIdKey to primaryTask.occurrenceId
                                )
                            ))
                    ) {
                        Text(
                            text = getRelativeTimeSpan(primaryTask.date, primaryTask.time),
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = "${primaryTask.getPriorityEmoji()} ${primaryTask.title}",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Text(
                            text = "✓ Complete",
                            style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            modifier = GlanceModifier
                                .background(GlanceTheme.colors.primary)
                                .cornerRadius(8.dp)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                .clickable(actionRunCallback<CompleteTaskActionCallback>(
                                    actionParametersOf(CompleteTaskActionCallback.TargetOccurrenceIdKey to primaryTask.occurrenceId)
                                ))
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = "⏰ 5m",
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            modifier = GlanceModifier
                                .background(GlanceTheme.colors.surface)
                                .cornerRadius(8.dp)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                .clickable(actionRunCallback<SnoozeTaskActionCallback>(
                                    actionParametersOf(SnoozeTaskActionCallback.TargetOccurrenceIdKey to primaryTask.occurrenceId)
                                ))
                        )
                    }
                }
            } else {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎉 All clear!",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }

    /**
     * Medium 3x2 / 4x2 productivity layout with compact personality.
     */
    @Composable
    private fun MediumWidgetLayout(state: WidgetUiState) {
        val tasks = state.pendingTasks

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.background)
                .padding(12.dp),
            verticalAlignment = Alignment.Vertical.Top
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = "Tsk.R",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                Box(
                    modifier = GlanceModifier
                        .background(GlanceTheme.colors.primaryContainer)
                        .cornerRadius(16.dp)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clickable(actionStartActivity<MainActivity>(actionParametersOf(OpenAddSheetKey to true))),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                        Image(
                            provider = ImageProvider(AndroidR.drawable.ic_input_add),
                            contentDescription = null,
                            modifier = GlanceModifier.size(16.dp)
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Text(
                            text = "Add",
                            style = TextStyle(
                                color = GlanceTheme.colors.onPrimaryContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            if (tasks.isNotEmpty()) {
                val primaryTask = tasks[0]
                val remainingTasks = tasks.drop(1).take(1)

                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(GlanceTheme.colors.surfaceVariant)
                        .cornerRadius(16.dp)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Column(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .clickable(actionStartActivity<MainActivity>(
                                    actionParametersOf(
                                        ReminderIdKey to primaryTask.reminderId,
                                        OccurrenceIdKey to primaryTask.occurrenceId
                                    )
                                ))
                        ) {
                            Text(
                                text = getRelativeTimeSpan(primaryTask.date, primaryTask.time),
                                style = TextStyle(
                                    color = GlanceTheme.colors.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "${primaryTask.getPriorityEmoji()} ${primaryTask.title}",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                        }

                        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                            Text(
                                text = "✓ Complete",
                                style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                modifier = GlanceModifier
                                    .background(GlanceTheme.colors.primary)
                                    .cornerRadius(10.dp)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .clickable(actionRunCallback<CompleteTaskActionCallback>(
                                        actionParametersOf(CompleteTaskActionCallback.TargetOccurrenceIdKey to primaryTask.occurrenceId)
                                    ))
                            )
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Text(
                                text = "⏰ 5m",
                                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                modifier = GlanceModifier
                                    .background(GlanceTheme.colors.surface)
                                    .cornerRadius(10.dp)
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .clickable(actionRunCallback<SnoozeTaskActionCallback>(
                                        actionParametersOf(SnoozeTaskActionCallback.TargetOccurrenceIdKey to primaryTask.occurrenceId)
                                    ))
                            )
                        }
                    }
                }

                remainingTasks.forEach { task ->
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Row(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .clickable(actionStartActivity<MainActivity>(
                                    actionParametersOf(
                                        ReminderIdKey to task.reminderId,
                                        OccurrenceIdKey to task.occurrenceId
                                    )
                                )),
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            Text(
                                text = task.getPriorityEmoji(),
                                style = TextStyle(fontSize = 12.sp)
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            Text(
                                text = task.title,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                modifier = GlanceModifier.defaultWeight()
                            )
                            Text(
                                text = getRelativeTimeSpan(task.date, task.time),
                                style = TextStyle(
                                    color = GlanceTheme.colors.secondary,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(
                            text = "✓",
                            style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                            modifier = GlanceModifier
                                .background(GlanceTheme.colors.primaryContainer)
                                .cornerRadius(8.dp)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .clickable(actionRunCallback<CompleteTaskActionCallback>(
                                    actionParametersOf(CompleteTaskActionCallback.TargetOccurrenceIdKey to task.occurrenceId)
                                ))
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "💬 \"${state.personalityQuote}\"",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic
                    ),
                    maxLines = 1
                )
            } else {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                        Text(
                            text = "🎉 All caught up!",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = "💬 \"${state.personalityQuote}\"",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Large 4x3 / 4x4 expanded productivity layout with Plan progress & Personality.
     */
    @Composable
    private fun LargeWidgetLayout(state: WidgetUiState) {
        val tasks = state.pendingTasks

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.background)
                .padding(14.dp),
            verticalAlignment = Alignment.Vertical.Top
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Tsk.R Dashboard",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = if (state.overdueCount > 0) "${state.totalPendingCount} pending (${state.overdueCount} overdue)" else "${state.totalPendingCount} pending tasks",
                        style = TextStyle(
                            color = if (state.overdueCount > 0) GlanceTheme.colors.error else GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Box(
                    modifier = GlanceModifier
                        .background(GlanceTheme.colors.primaryContainer)
                        .cornerRadius(16.dp)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clickable(actionStartActivity<MainActivity>(actionParametersOf(OpenAddSheetKey to true))),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                        Image(
                            provider = ImageProvider(AndroidR.drawable.ic_input_add),
                            contentDescription = null,
                            modifier = GlanceModifier.size(16.dp)
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Text(
                            text = "Add",
                            style = TextStyle(
                                color = GlanceTheme.colors.onPrimaryContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            // Active Plan Progress Badge (if assigned to an active plan)
            if (state.hasActivePlan()) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(GlanceTheme.colors.secondaryContainer)
                        .cornerRadius(12.dp)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = "${state.activePlanIcon ?: "🎯"} ${state.activePlanName}",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSecondaryContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = "${state.activePlanCompletedTasks}/${state.activePlanTotalTasks} tasks (${state.getPlanProgressPercent()}%)",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSecondaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                Spacer(modifier = GlanceModifier.height(6.dp))
            }

            // Personality Quote Card
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(GlanceTheme.colors.surfaceVariant)
                    .cornerRadius(12.dp)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "💬 \"${state.personalityQuote}\"",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 2
                )
            }
            Spacer(modifier = GlanceModifier.height(8.dp))

            if (tasks.isNotEmpty()) {
                val primaryTask = tasks[0]
                val remainingTasks = tasks.drop(1).take(3)

                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(GlanceTheme.colors.surfaceVariant)
                        .cornerRadius(14.dp)
                        .padding(12.dp)
                ) {
                    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
                        Column(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .clickable(actionStartActivity<MainActivity>(
                                    actionParametersOf(
                                        ReminderIdKey to primaryTask.reminderId,
                                        OccurrenceIdKey to primaryTask.occurrenceId
                                    )
                                ))
                        ) {
                            Row(modifier = GlanceModifier.fillMaxWidth()) {
                                Text(
                                    text = getRelativeTimeSpan(primaryTask.date, primaryTask.time),
                                    style = TextStyle(
                                        color = GlanceTheme.colors.primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = GlanceModifier.defaultWeight()
                                )
                                if (!primaryTask.planName.isNullOrBlank()) {
                                    Text(
                                        text = "${primaryTask.planIcon ?: "🎯"} ${primaryTask.planName}",
                                        style = TextStyle(
                                            color = GlanceTheme.colors.secondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            Text(
                                text = "${primaryTask.getPriorityEmoji()} ${primaryTask.title}",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.Vertical.CenterVertically,
                            modifier = GlanceModifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "✓ Complete",
                                style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                modifier = GlanceModifier
                                    .background(GlanceTheme.colors.primary)
                                    .cornerRadius(10.dp)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .clickable(actionRunCallback<CompleteTaskActionCallback>(
                                        actionParametersOf(CompleteTaskActionCallback.TargetOccurrenceIdKey to primaryTask.occurrenceId)
                                    ))
                            )
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Text(
                                text = "⏰ 5m",
                                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                modifier = GlanceModifier
                                    .background(GlanceTheme.colors.surface)
                                    .cornerRadius(10.dp)
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .clickable(actionRunCallback<SnoozeTaskActionCallback>(
                                        actionParametersOf(SnoozeTaskActionCallback.TargetOccurrenceIdKey to primaryTask.occurrenceId)
                                    ))
                            )
                        }
                    }
                }

                remainingTasks.forEach { task ->
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Row(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .clickable(actionStartActivity<MainActivity>(
                                    actionParametersOf(
                                        ReminderIdKey to task.reminderId,
                                        OccurrenceIdKey to task.occurrenceId
                                    )
                                )),
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            Text(
                                text = task.getPriorityEmoji(),
                                style = TextStyle(fontSize = 12.sp)
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            Text(
                                text = task.title,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                modifier = GlanceModifier.defaultWeight()
                            )
                            if (!task.planName.isNullOrBlank()) {
                                Text(
                                    text = task.planName,
                                    style = TextStyle(
                                        color = GlanceTheme.colors.secondary,
                                        fontSize = 11.sp
                                    )
                                )
                                Spacer(modifier = GlanceModifier.width(8.dp))
                            }
                            Text(
                                text = getRelativeTimeSpan(task.date, task.time),
                                style = TextStyle(
                                    color = GlanceTheme.colors.secondary,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(
                            text = "✓",
                            style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                            modifier = GlanceModifier
                                .background(GlanceTheme.colors.primaryContainer)
                                .cornerRadius(8.dp)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .clickable(actionRunCallback<CompleteTaskActionCallback>(
                                    actionParametersOf(CompleteTaskActionCallback.TargetOccurrenceIdKey to task.occurrenceId)
                                ))
                        )
                    }
                }
            } else {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                        Text(
                            text = "🎉 All clear on your schedule!",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = "No pending tasks right now.",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

class TskRWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TskRWidget()
}

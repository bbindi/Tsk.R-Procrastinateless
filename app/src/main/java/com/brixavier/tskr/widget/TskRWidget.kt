package com.brixavier.tskr.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.brixavier.tskr.MainActivity
import com.brixavier.tskr.R
import com.brixavier.tskr.data.ReminderDatabase
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.ui.getRelativeTimeSpan

class TskRWidget : GlanceAppWidget() {

    companion object {
        val OpenAddSheetKey = ActionParameters.Key<Boolean>(MainActivity.EXTRA_OPEN_ADD_SHEET)
        val ReminderIdKey = ActionParameters.Key<String>(MainActivity.EXTRA_OPEN_REMINDER_ID)
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = ReminderDatabase.getDatabase(context)
        val reminders = database.reminderDao().getUpcomingRemindersLimit(3)

        provideContent {
            GlanceTheme {
                TskRWidgetContent(reminders)
            }
        }
    }

    @Composable
    private fun TskRWidgetContent(reminders: List<Reminder>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.background)
                .padding(12.dp),
            verticalAlignment = Alignment.Vertical.Top
        ) {
            // 1. Redesigned Header
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
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
                
                // Styled Quick-Add Button
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
                            provider = ImageProvider(android.R.drawable.ic_input_add),
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

            if (reminders.isNotEmpty()) {
                val primaryTask = reminders[0]
                val remainingTasks = reminders.drop(1)

                // 2. Primary Task Card
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(GlanceTheme.colors.surfaceVariant)
                        .cornerRadius(16.dp)
                        .padding(12.dp)
                        .clickable(actionStartActivity<MainActivity>(
                            actionParametersOf(ReminderIdKey to primaryTask.id)
                        ))
                ) {
                    Text(
                        text = getRelativeTimeSpan(primaryTask.date, primaryTask.time),
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "${primaryTask.getPriorityEmoji()} ${primaryTask.title}",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                }

                // 3. Secondary Task Items
                remainingTasks.forEach { task ->
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .clickable(actionStartActivity<MainActivity>(
                                actionParametersOf(ReminderIdKey to task.id)
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
                }
            } else {
                // 4. Centered Empty State
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
                        Text(
                            text = "Go celebrate with a snack.",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 12.sp
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

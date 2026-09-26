package com.brixavier.tskr

import android.app.KeyguardManager
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import com.brixavier.tskr.data.ReminderDatabase
import com.brixavier.tskr.model.Plan
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.model.ReminderOccurrence
import com.brixavier.tskr.personality.ActionType
import com.brixavier.tskr.personality.PersonalityContext
import com.brixavier.tskr.personality.PersonalityEngine
import com.brixavier.tskr.personality.PersonalityIntensity
import com.brixavier.tskr.personality.UrgencyLevel
import com.brixavier.tskr.scheduler.ReminderScheduler
import com.brixavier.tskr.sound.NotificationIdHelper
import com.brixavier.tskr.sound.SharedPreferencesSoundStore
import com.brixavier.tskr.sound.SoundResolver
import com.brixavier.tskr.sound.SoundType
import com.brixavier.tskr.ui.getRelativeTimeSpan
import com.brixavier.tskr.ui.theme.ReminderAppTheme
import com.brixavier.tskr.ui.theme.StatusOverdueDark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

val Cookie9Polygon = RoundedPolygon.star(
    numVerticesPerRadius = 9,
    radius = 1f,
    innerRadius = 0.68f,
    rounding = CornerRounding(radius = 0.45f, smoothing = 0.25f)
)

val Cookie7Polygon = RoundedPolygon.star(
    numVerticesPerRadius = 7,
    radius = 1f,
    innerRadius = 0.72f,
    rounding = CornerRounding(radius = 0.4f, smoothing = 0.2f)
)

data class MissionModePalette(
    val background: Color,
    val stageBadgeContainer: Color,
    val stageBadgeContent: Color,
    val layer1Color: Color,
    val layer2Color: Color,
    val layer3Color: Color,
    val heroText: Color,
    val timePillContainer: Color,
    val timePillText: Color,
    val ambientOutline: Color,
    val quoteCardContainer: Color,
    val quoteCardText: Color,
    val primaryButtonContainer: Color,
    val primaryButtonContent: Color
)

@Composable
fun rememberMissionModePalette(alarmStage: String, isCompleted: Boolean): MissionModePalette {
    val isDark = isSystemInDarkTheme()
    val colorScheme = MaterialTheme.colorScheme

    return remember(alarmStage, isCompleted, isDark, colorScheme) {
        if (isCompleted) {
            MissionModePalette(
                background = if (isDark) Color(0xFF0D1F12) else Color(0xFFE8F5E9),
                stageBadgeContainer = colorScheme.tertiaryContainer,
                stageBadgeContent = colorScheme.onTertiaryContainer,
                layer1Color = colorScheme.tertiary.copy(alpha = if (isDark) 0.20f else 0.15f),
                layer2Color = colorScheme.tertiary.copy(alpha = if (isDark) 0.40f else 0.30f),
                layer3Color = colorScheme.tertiaryContainer,
                heroText = colorScheme.onTertiaryContainer,
                timePillContainer = colorScheme.surface.copy(alpha = 0.4f),
                timePillText = colorScheme.onTertiaryContainer,
                ambientOutline = colorScheme.tertiary.copy(alpha = if (isDark) 0.12f else 0.20f),
                quoteCardContainer = colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.8f else 0.85f),
                quoteCardText = colorScheme.onSurfaceVariant,
                primaryButtonContainer = colorScheme.tertiary,
                primaryButtonContent = colorScheme.onTertiary
            )
        } else {
            when (alarmStage) {
                ReminderScheduler.STAGE_7_DAYS -> MissionModePalette(
                    background = if (isDark) Color(0xFF0A0E29) else Color(0xFFF3F4FB),
                    stageBadgeContainer = colorScheme.surfaceVariant,
                    stageBadgeContent = colorScheme.onSurfaceVariant,
                    layer1Color = colorScheme.outlineVariant.copy(alpha = if (isDark) 0.15f else 0.25f),
                    layer2Color = colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.5f else 0.7f),
                    layer3Color = colorScheme.surfaceVariant,
                    heroText = colorScheme.onSurfaceVariant,
                    timePillContainer = colorScheme.surface.copy(alpha = 0.4f),
                    timePillText = colorScheme.onSurfaceVariant,
                    ambientOutline = colorScheme.primary.copy(alpha = if (isDark) 0.10f else 0.18f),
                    quoteCardContainer = colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.8f else 0.85f),
                    quoteCardText = colorScheme.onSurfaceVariant,
                    primaryButtonContainer = colorScheme.primary,
                    primaryButtonContent = colorScheme.onPrimary
                )
                ReminderScheduler.STAGE_24_HOURS -> MissionModePalette(
                    background = if (isDark) Color(0xFF140D26) else Color(0xFFF6F0FC),
                    stageBadgeContainer = colorScheme.secondaryContainer,
                    stageBadgeContent = colorScheme.onSecondaryContainer,
                    layer1Color = colorScheme.secondary.copy(alpha = if (isDark) 0.25f else 0.20f),
                    layer2Color = colorScheme.secondaryContainer.copy(alpha = if (isDark) 0.6f else 0.75f),
                    layer3Color = colorScheme.secondaryContainer,
                    heroText = colorScheme.onSecondaryContainer,
                    timePillContainer = colorScheme.surface.copy(alpha = 0.4f),
                    timePillText = colorScheme.onSecondaryContainer,
                    ambientOutline = colorScheme.secondary.copy(alpha = if (isDark) 0.12f else 0.22f),
                    quoteCardContainer = colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.8f else 0.85f),
                    quoteCardText = colorScheme.onSurfaceVariant,
                    primaryButtonContainer = colorScheme.primary,
                    primaryButtonContent = colorScheme.onPrimary
                )
                else -> MissionModePalette( // DUE NOW
                    background = if (isDark) Color(0xFF1A0A0A) else Color(0xFFFFF0F0),
                    stageBadgeContainer = colorScheme.errorContainer,
                    stageBadgeContent = colorScheme.onErrorContainer,
                    layer1Color = colorScheme.error.copy(alpha = if (isDark) 0.18f else 0.15f),
                    layer2Color = colorScheme.error.copy(alpha = if (isDark) 0.40f else 0.30f),
                    layer3Color = if (isDark) StatusOverdueDark else colorScheme.errorContainer,
                    heroText = if (isDark) Color.White else colorScheme.onErrorContainer,
                    timePillContainer = Color.Black.copy(alpha = if (isDark) 0.25f else 0.12f),
                    timePillText = if (isDark) Color.White else colorScheme.onErrorContainer,
                    ambientOutline = colorScheme.error.copy(alpha = if (isDark) 0.15f else 0.25f),
                    quoteCardContainer = colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.8f else 0.85f),
                    quoteCardText = colorScheme.onSurfaceVariant,
                    primaryButtonContainer = colorScheme.primary,
                    primaryButtonContent = colorScheme.onPrimary
                )
            }
        }
    }
}

/**
 * Striking, full-screen Alarm Overlay Activity - MISSION MODE.
 * Updated in V2-H.1 with hybrid palette & ambient background shapes.
 */
class AlarmActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var tts: TextToSpeech? = null
    private var missionQuote: String = ""
    
    private val database by lazy { ReminderDatabase.getDatabase(this) }
    private val scheduler by lazy { ReminderScheduler(this) }
    private val notificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureLockScreenFlags()

        val occurrenceId = intent.getStringExtra(ReminderScheduler.EXTRA_OCCURRENCE_ID)
            ?: intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_ID)
            ?: ""
        val reminderTitle = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_TITLE) ?: "Mission Alert!"
        val alarmStage = intent.getStringExtra(ReminderScheduler.EXTRA_ALARM_STAGE) ?: ReminderScheduler.STAGE_DUE_NOW
        val priority = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_PRIORITY)
        
        val isImportant = Reminder.isImportant(priority)
        val sharedPref = getSharedPreferences("tsk_r_prefs", MODE_PRIVATE)
        val streak = sharedPref.getInt("streak_count", 0)

        val ctxAction = when (alarmStage) {
            ReminderScheduler.STAGE_7_DAYS, ReminderScheduler.STAGE_24_HOURS -> ActionType.MISSION_MODE_WARNING
            else -> ActionType.MISSION_MODE_DUE
        }
        val ctx = PersonalityContext(
            actionType = ctxAction,
            intensity = if (isImportant) PersonalityIntensity.MISSION else PersonalityIntensity.ACCOUNTABILITY,
            urgency = if (isImportant) UrgencyLevel.IMPORTANT else UrgencyLevel.NORMAL,
            streak = streak,
            timeOfDayHour = LocalTime.now().hour
        )
        val personalityEngine = PersonalityEngine()
        missionQuote = personalityEngine.getMessage(ctx).text
        
        tts = TextToSpeech(this, this)

        startAlarmSignals()

        setContent {
            ReminderAppTheme(darkTheme = isSystemInDarkTheme()) {
                var occurrence by remember { mutableStateOf<ReminderOccurrence?>(null) }
                var reminder by remember { mutableStateOf<Reminder?>(null) }
                var plan by remember { mutableStateOf<Plan?>(null) }
                var dynamicQuote by remember { mutableStateOf(missionQuote) }
                var isCompletedState by remember { mutableStateOf(false) }
                var isCompletingState by remember { mutableStateOf(false) }

                LaunchedEffect(occurrenceId) {
                    if (occurrenceId.isNotEmpty()) {
                        val loadedOcc = database.reminderDao().getOccurrenceById(occurrenceId)

                        // HARD OCCURRENCE SAFETY GUARD: Reject stale occurrences
                        if (loadedOcc == null || !loadedOcc.isPending()) {
                            Log.d("AlarmActivity", "Occurrence $occurrenceId is missing or not pending (status: ${loadedOcc?.status}). Rejecting stale launch.")
                            cancelNotification(occurrenceId)
                            dismissAlarm()
                            return@LaunchedEffect
                        }

                        occurrence = loadedOcc
                        val remId = loadedOcc.templateId ?: occurrenceId
                        reminder = database.reminderDao().getReminderById(remId)

                        val pId = loadedOcc.planId ?: reminder?.planId
                        var planProgressRatio = 0f
                        var planTotalTasks = 0
                        var planCompletedTasks = 0

                        if (!pId.isNullOrBlank()) {
                            plan = database.reminderDao().getPlanById(pId)
                            if (plan != null) {
                                val progress = database.reminderDao().getPlanProgress(plan!!.id)
                                planTotalTasks = progress.totalTasks
                                planCompletedTasks = progress.completedTasks
                                planProgressRatio = progress.progressRatio
                            }
                        }

                        var overdueMinutes = 0
                        try {
                            val dtStr = "${loadedOcc.date} ${loadedOcc.time}"
                            val scheduledDt = LocalDateTime.parse(dtStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                            val nowLocal = LocalDateTime.now()
                            if (nowLocal.isAfter(scheduledDt)) {
                                overdueMinutes = Duration.between(scheduledDt, nowLocal).toMinutes().toInt()
                            }
                        } catch (_: Exception) {}

                        val isRecurring = loadedOcc.templateId != null || reminder?.isRecurring() == true
                        val snoozeCount = if (loadedOcc.snoozedUntilMs != null && loadedOcc.snoozedUntilMs > 0) 1 else 0

                        val enrichedCtx = PersonalityContext(
                            actionType = ctxAction,
                            intensity = if (isImportant) PersonalityIntensity.MISSION else PersonalityIntensity.ACCOUNTABILITY,
                            urgency = if (isImportant) UrgencyLevel.IMPORTANT else UrgencyLevel.NORMAL,
                            isRecurring = isRecurring,
                            streak = streak,
                            timeOfDayHour = LocalTime.now().hour,
                            overdueMinutes = overdueMinutes,
                            snoozeCount = snoozeCount,
                            planProgressRatio = planProgressRatio,
                            planTotalTasks = planTotalTasks,
                            planCompletedTasks = planCompletedTasks
                        )

                        dynamicQuote = personalityEngine.getMessage(enrichedCtx).text
                    }
                }

                AlarmScreen(
                    title = reminderTitle,
                    reminder = reminder,
                    plan = plan,
                    quote = dynamicQuote,
                    alarmStage = alarmStage,
                    isCompleted = isCompletedState,
                    isCompleting = isCompletingState,
                    onStartNow = {
                        if (!isCompletingState && !isCompletedState) {
                            isCompletingState = true
                            scope.launch {
                                val targetOccId = occurrence?.id ?: occurrenceId
                                val isRecurring = occurrence?.templateId != null || reminder?.isRecurring() == true

                                if (targetOccId.isNotEmpty()) {
                                    val nextOcc = database.reminderDao().completeOccurrenceTransaction(targetOccId)
                                    scheduler.cancelOccurrenceAlarms(targetOccId)
                                    cancelNotification(targetOccId)
                                    if (nextOcc != null) {
                                        scheduler.scheduleOccurrenceAlarms(nextOcc)
                                    }
                                } else if (reminder != null) {
                                    database.reminderDao().update(reminder!!.copy(isActive = false))
                                    scheduler.cancelAlarms(reminder!!)
                                    cancelNotification(reminder!!.id)
                                }

                                // Resolve completion quote
                                val completionCtx = PersonalityContext(
                                    actionType = ActionType.MISSION_MODE_COMPLETION,
                                    intensity = PersonalityIntensity.MISSION,
                                    streak = streak,
                                    isRecurring = isRecurring
                                )
                                dynamicQuote = personalityEngine.getMessage(completionCtx).text
                                isCompletedState = true

                                triggerCompletionHaptic()
                                delay(800)
                                dismissAlarm()
                            }
                        }
                    },
                    onSnooze = {
                        if (!isCompletingState && !isCompletedState) {
                            scope.launch {
                                if (occurrence != null) {
                                    snoozeOccurrence(occurrence!!)
                                } else if (reminder != null) {
                                    snoozeReminder(reminder!!)
                                } else {
                                    dismissAlarm()
                                }
                            }
                        }
                    },
                    onDismiss = {
                        if (!isCompletingState && !isCompletedState) {
                            val targetId = occurrence?.id ?: reminder?.id ?: occurrenceId
                            if (targetId.isNotEmpty()) {
                                cancelNotification(targetId)
                            }
                            dismissAlarm()
                        }
                    }
                )
            }
        }
    }

    private fun cancelNotification(targetId: String) {
        if (targetId.isNotEmpty()) {
            notificationManager.cancel(NotificationIdHelper.getNotificationId(targetId, ReminderScheduler.STAGE_7_DAYS))
            notificationManager.cancel(NotificationIdHelper.getNotificationId(targetId, ReminderScheduler.STAGE_24_HOURS))
            notificationManager.cancel(NotificationIdHelper.getNotificationId(targetId, ReminderScheduler.STAGE_DUE_NOW))
            notificationManager.cancel(targetId.hashCode())
        }
    }

    private fun triggerCompletionHaptic() {
        try {
            val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(VibratorManager::class.java)
                vibratorManager?.defaultVibrator
            } else {
                getSystemService(Vibrator::class.java)
            }
            vib?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {}
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.speak(missionQuote, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun snoozeOccurrence(occurrence: ReminderOccurrence) {
        scope.launch {
            val snoozeTime = LocalDateTime.now().plusMinutes(5)
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
            val snoozeEpochMs = snoozeTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val updated = occurrence.copy(
                date = snoozeTime.format(dateFormatter),
                time = snoozeTime.format(timeFormatter),
                snoozedUntilMs = snoozeEpochMs,
                snoozeCount = occurrence.snoozeCount + 1
            )
            database.reminderDao().updateOccurrence(updated)
            scheduler.scheduleOccurrenceAlarms(updated)
            cancelNotification(occurrence.id)
            finish()
        }
    }

    private fun snoozeReminder(reminder: Reminder) {
        scope.launch {
            val snoozeTime = LocalDateTime.now().plusMinutes(5)
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
            
            val updated = reminder.copy(
                date = snoozeTime.format(dateFormatter),
                time = snoozeTime.format(timeFormatter)
            )
            database.reminderDao().update(updated)
            scheduler.scheduleAlarms(updated)
            cancelNotification(reminder.id)
            finish()
        }
    }

    /**
     * Appends flags to display over secure lock screen, turn screen on, and unlock keyguard.
     */
    @Suppress("DEPRECATION")
    private fun configureLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /**
     * Triggers hardware vibrations and rings the default alarm ringtone.
     */
    private fun startAlarmSignals() {
        try {
            val soundStore = SharedPreferencesSoundStore(this)
            val alarmUri = SoundResolver.resolveSoundUri(this, soundStore, SoundType.MISSION_MODE)

            ringtone = try {
                RingtoneManager.getRingtone(applicationContext, alarmUri)?.apply {
                    audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    play()
                }
            } catch (_: Exception) {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                RingtoneManager.getRingtone(applicationContext, fallbackUri)?.apply {
                    audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    play()
                }
            }

            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(VibratorManager::class.java)
                vibratorManager?.defaultVibrator
            } else {
                getSystemService(Vibrator::class.java)
            }

            val pattern = longArrayOf(0, 150, 100, 150, 600) // Rhythmic heartbeat pattern
            vibrator?.vibrate(
                VibrationEffect.createWaveform(pattern, 0)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarmSignals() {
        try {
            ringtone?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
            vibrator?.cancel()
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dismissAlarm() {
        stopAlarmSignals()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSignals()
    }
}

/**
 * Full-Bleed 3-Layer Rotating Cookie Container for the Hero Task Title.
 * Uses Material 3 Expressive pillowy wave polygons with multi-layer depth.
 */
@Composable
fun OrganicTaskHeroContainer(
    modifier: Modifier = Modifier,
    palette: MissionModePalette,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isReducedMotion = remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        } catch (_: Exception) {
            false
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "cookieRotation")

    // Clockwise rotation (36s cycle)
    val rotation36s by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(36000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation36s"
    )

    // Counter-clockwise rotation (22s cycle)
    val rotation22s by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation22s"
    )

    // Slow clockwise rotation (55s cycle)
    val rotation55s by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(55000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation55s"
    )

    val layer1Angle = if (isReducedMotion) 0f else rotation36s
    val layer2Angle = if (isReducedMotion) 0f else rotation22s
    val layer3Angle = if (isReducedMotion) 0f else rotation55s

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Layer 1: Full-Bleed Outer Ambient Wave Cookie (520.dp, Clockwise 36s)
        Box(
            modifier = Modifier
                .size(520.dp)
                .rotate(layer1Angle)
                .drawBehind {
                    val path = Cookie9Polygon.toPath().asComposePath()
                    val w = size.width
                    val h = size.height
                    val scaleX = w / 2f
                    val scaleY = h / 2f
                    drawContext.canvas.save()
                    drawContext.canvas.translate(scaleX, scaleY)
                    drawContext.canvas.scale(scaleX, scaleY)
                    drawPath(path = path, color = palette.layer1Color)
                    drawContext.canvas.restore()
                }
        )

        // Layer 2: Middle Swirling Cookie (440.dp, Counter-clockwise 22s)
        Box(
            modifier = Modifier
                .size(440.dp)
                .rotate(layer2Angle)
                .drawBehind {
                    val path = Cookie7Polygon.toPath().asComposePath()
                    val w = size.width
                    val h = size.height
                    val scaleX = w / 2f
                    val scaleY = h / 2f
                    drawContext.canvas.save()
                    drawContext.canvas.translate(scaleX, scaleY)
                    drawContext.canvas.scale(scaleX, scaleY)
                    drawPath(path = path, color = palette.layer2Color)
                    drawContext.canvas.restore()
                }
        )

        // Layer 3: Hero Core Cookie (360.dp, Slow Clockwise 55s)
        Box(
            modifier = Modifier
                .size(360.dp)
                .rotate(layer3Angle)
                .drawBehind {
                    val path = Cookie9Polygon.toPath().asComposePath()
                    val w = size.width
                    val h = size.height
                    val scaleX = w / 2f
                    val scaleY = h / 2f
                    drawContext.canvas.save()
                    drawContext.canvas.translate(scaleX, scaleY)
                    drawContext.canvas.scale(scaleX, scaleY)
                    drawPath(path = path, color = palette.layer3Color)
                    drawContext.canvas.restore()
                }
        )

        // Foreground Content Box (Transparent, high-contrast text anchor)
        Box(
            modifier = Modifier
                .size(340.dp)
                .padding(horizontal = 32.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
fun AlarmScreen(
    title: String,
    reminder: Reminder?,
    plan: Plan?,
    quote: String,
    alarmStage: String,
    isCompleted: Boolean = false,
    isCompleting: Boolean = false,
    onStartNow: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = rememberMissionModePalette(alarmStage = alarmStage, isCompleted = isCompleted)
    val context = LocalContext.current
    val isReducedMotion = remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        } catch (_: Exception) {
            false
        }
    }

    val ambientTransition = rememberInfiniteTransition(label = "ambientCookieRotation")

    val ambientRotation1 by ambientTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambient1"
    )

    val ambientRotation2 by ambientTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(45000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambient2"
    )

    val ambientRotation3 by ambientTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(50000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambient3"
    )

    val ambientAngle1 = if (isReducedMotion) 0f else ambientRotation1
    val ambientAngle2 = if (isReducedMotion) 0f else ambientRotation2
    val ambientAngle3 = if (isReducedMotion) 0f else ambientRotation3

    val screenTitle = when {
        isCompleted -> "🏆 MISSION ACCOMPLISHED"
        alarmStage == ReminderScheduler.STAGE_7_DAYS -> "⚠️ 7-DAY EARLY WARNING"
        alarmStage == ReminderScheduler.STAGE_24_HOURS -> "🚨 24-HOUR COUNTDOWN"
        else -> "🎯 CURRENT MISSION (DUE NOW)"
    }

    val primaryButtonText = when {
        isCompleted -> "✓ Completed!"
        alarmStage == ReminderScheduler.STAGE_7_DAYS -> "Acknowledge Warning"
        alarmStage == ReminderScheduler.STAGE_24_HOURS -> "I'm On It!"
        else -> "🚀 Complete Task"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        // Ambient Outlined Background Cookie 1 (Top-Right, 560.dp, 60s Clockwise)
        Box(
            modifier = Modifier
                .size(560.dp)
                .offset(x = 120.dp, y = (-120).dp)
                .rotate(ambientAngle1)
                .drawBehind {
                    val path = Cookie7Polygon.toPath().asComposePath()
                    val w = size.width
                    val h = size.height
                    val scaleX = w / 2f
                    val scaleY = h / 2f
                    drawContext.canvas.save()
                    drawContext.canvas.translate(scaleX, scaleY)
                    drawContext.canvas.scale(scaleX, scaleY)
                    drawPath(
                        path = path,
                        color = palette.ambientOutline,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawContext.canvas.restore()
                }
        )

        // Ambient Outlined Background Cookie 2 (Bottom-Left, 240.dp, 45s Counter-Clockwise)
        Box(
            modifier = Modifier
                .size(240.dp)
                .offset(x = (-100).dp, y = 380.dp)
                .rotate(ambientAngle2)
                .drawBehind {
                    val path = Cookie9Polygon.toPath().asComposePath()
                    val w = size.width
                    val h = size.height
                    val scaleX = w / 2f
                    val scaleY = h / 2f
                    drawContext.canvas.save()
                    drawContext.canvas.translate(scaleX, scaleY)
                    drawContext.canvas.scale(scaleX, scaleY)
                    drawPath(
                        path = path,
                        color = palette.ambientOutline,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawContext.canvas.restore()
                }
        )

        // Ambient Outlined Background Cookie 3 (Mid-Right, 180.dp, 50s Clockwise)
        Box(
            modifier = Modifier
                .size(180.dp)
                .offset(x = 160.dp, y = 260.dp)
                .rotate(ambientAngle3)
                .drawBehind {
                    val path = Cookie7Polygon.toPath().asComposePath()
                    val w = size.width
                    val h = size.height
                    val scaleX = w / 2f
                    val scaleY = h / 2f
                    drawContext.canvas.save()
                    drawContext.canvas.translate(scaleX, scaleY)
                    drawContext.canvas.scale(scaleX, scaleY)
                    drawPath(
                        path = path,
                        color = palette.ambientOutline,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawContext.canvas.restore()
                }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Mission Stage Badge & Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    color = palette.stageBadgeContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = screenTitle,
                        color = palette.stageBadgeContent,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                // 2. HERO TASK TITLE INSIDE 3-LAYER ROTATING 9-SIDED COOKIE CONTAINER
                OrganicTaskHeroContainer(
                    palette = palette
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = palette.heroText,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = title,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            color = palette.heroText,
                            textAlign = TextAlign.Center,
                            lineHeight = 48.sp
                        )

                        // Scheduled Time / Relative Span
                        if (reminder != null && !isCompleted) {
                            Surface(
                                color = palette.timePillContainer,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "⏰ ${getRelativeTimeSpan(reminder.date, reminder.time)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.timePillText,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 3. Plan Context Badge (If attached to a Plan)
                if (plan != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = plan.icon.ifBlank { "🎯" }, fontSize = 14.sp)
                            Text(
                                text = "Part of ${plan.name}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 4. Personality Quote Container (Clean Streamlined Card)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = palette.quoteCardContainer
                )
            ) {
                Text(
                    text = "\"$quote\"",
                    style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                    color = palette.quoteCardText,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                )
            }

            // 5. Action Buttons (Primary Complete vs Secondary Snooze)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = if (alarmStage == ReminderScheduler.STAGE_DUE_NOW) onStartNow else onDismiss,
                    enabled = !isCompleting && !isCompleted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primaryButtonContainer,
                        contentColor = palette.primaryButtonContent
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Text(
                        text = primaryButtonText.uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp
                    )
                }

                if (alarmStage == ReminderScheduler.STAGE_DUE_NOW && !isCompleted) {
                    OutlinedButton(
                        onClick = onSnooze,
                        enabled = !isCompleting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "😅 Give Me 5 Minutes",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

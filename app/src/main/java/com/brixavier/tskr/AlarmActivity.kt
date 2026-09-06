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
import android.speech.tts.TextToSpeech
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brixavier.tskr.data.ReminderDatabase
import com.brixavier.tskr.engine.PersonalityEngine
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.scheduler.ReminderScheduler
import com.brixavier.tskr.ui.getRelativeTimeSpan
import com.brixavier.tskr.ui.theme.ReminderAppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Striking, full-screen Alarm Overlay Activity - MISSION MODE.
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

        val reminderId = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_ID) ?: ""
        val reminderTitle = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_TITLE) ?: "Mission Alert!"
        val alarmStage = intent.getStringExtra(ReminderScheduler.EXTRA_ALARM_STAGE) ?: ReminderScheduler.STAGE_DUE_NOW
        
        missionQuote = when (alarmStage) {
            ReminderScheduler.STAGE_7_DAYS -> "Warning! Your task is one week out. Do not let this surprise you!"
            ReminderScheduler.STAGE_24_HOURS -> "24 hours remaining! Panic mode optional, preparation mandatory!"
            else -> PersonalityEngine.getRandomQuote()
        }
        
        tts = TextToSpeech(this, this)

        startAlarmSignals()

        setContent {
            ReminderAppTheme(darkTheme = true) {
                var reminder by remember { mutableStateOf<Reminder?>(null) }
                
                LaunchedEffect(reminderId) {
                    if (reminderId.isNotEmpty()) {
                        reminder = database.reminderDao().getReminderById(reminderId)
                    }
                }

                AlarmScreen(
                    title = reminderTitle,
                    reminder = reminder,
                    quote = missionQuote,
                    alarmStage = alarmStage,
                    onStartNow = {
                        reminder?.let {
                            scope.launch {
                                database.reminderDao().update(it.copy(isActive = false))
                                scheduler.cancelAlarms(it)
                                cancelNotification(it.id)
                                finish()
                            }
                        } ?: dismissAlarm()
                    },
                    onSnooze = {
                        reminder?.let {
                            snoozeReminder(it)
                        } ?: dismissAlarm()
                    },
                    onDismiss = {
                        reminder?.let { cancelNotification(it.id) }
                        dismissAlarm()
                    }
                )
            }
        }
    }

    private fun cancelNotification(reminderId: String) {
        notificationManager.cancel(reminderId.hashCode())
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.speak(missionQuote, TextToSpeech.QUEUE_FLUSH, null, null)
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
            // A. Ringtone Initializer
            var alarmUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }

            ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                play()
            }

            // B. Continuous Rhythmic Vibration (Heavy pulsing)
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

    /**
     * Releases hardware sound, vibration and TTS.
     */
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

    /**
     * Releases hardware sound and vibration, then finishes Activity.
     */
    private fun dismissAlarm() {
        stopAlarmSignals()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSignals()
    }
}

@Composable
fun AlarmScreen(
    title: String,
    reminder: Reminder?,
    quote: String,
    alarmStage: String,
    onStartNow: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    // Elegant pulsing animation for warning icon/reward card
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Atmospheric Material You glow
    val ambientColor by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
        targetValue = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambientColor"
    )

    val screenTitle = when (alarmStage) {
        ReminderScheduler.STAGE_7_DAYS -> "⚠️ 7-DAY EARLY WARNING"
        ReminderScheduler.STAGE_24_HOURS -> "🚨 24-HOUR COUNTDOWN"
        else -> "🎯 CURRENT MISSION (DUE NOW)"
    }

    val bodyText = when (alarmStage) {
        ReminderScheduler.STAGE_7_DAYS -> "You have '$title' coming up in 7 DAYS. Stop pretending it's far away—start preparing today!"
        ReminderScheduler.STAGE_24_HOURS -> "You have '$title' in exactly 24 HOURS! Get ready, your future self is watching!"
        else -> "'$title' is DUE NOW! Do it now. Your future self has enough problems."
    }

    val primaryButtonText = when (alarmStage) {
        ReminderScheduler.STAGE_7_DAYS -> "Acknowledge Warning"
        ReminderScheduler.STAGE_24_HOURS -> "I'm On It!"
        else -> "🚀 Complete Task"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Atmospheric Glow Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ambientColor)
        )

        // Readability Scrim & Content Layer
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // 1. Mission Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = screenTitle,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    lineHeight = 48.sp
                )
                
                if (reminder != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "⏰ ${getRelativeTimeSpan(reminder.date, reminder.time)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 2. Body Text
            Text(
                text = bodyText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // 3. Reward Card (Pulsing)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(pulseScale),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Stars, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "POTENTIAL REWARD",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Peace of mind 😌",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 4. Personality Quote & Controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "\"$quote\"",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = if (alarmStage == ReminderScheduler.STAGE_DUE_NOW) onStartNow else onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Text(primaryButtonText.uppercase(), fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 1.sp)
                    }

                    if (alarmStage == ReminderScheduler.STAGE_DUE_NOW) {
                        OutlinedButton(
                            onClick = onSnooze,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "😅 Give Me 5 Minutes",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.brixavier.tskr

import android.app.KeyguardManager
import android.content.Context
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
                        dismissAlarm()
                    }
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.speak(missionQuote, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun snoozeReminder(reminder: Reminder) {
        scope.launch {
            val now = LocalDateTime.now().plusMinutes(5)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val parts = now.format(formatter).split(" ")
            val updated = reminder.copy(date = parts[0], time = parts[1])
            database.reminderDao().update(updated)
            scheduler.scheduleAlarms(updated)
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
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                play()
            }

            // B. Continuous Rhythmic Vibration (Heavy pulsing)
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            val pattern = longArrayOf(0, 150, 100, 150, 600) // Rhythmic heartbeat pattern
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(pattern, 0)
                )
            } else {
                vibrator?.vibrate(pattern, 0)
            }
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
    // Elegant pulsing animation for warning icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val ambientColor by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.background,
        targetValue = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ambientColor)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Mission Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 48.dp)
        ) {
            Text(
                text = screenTitle,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = title,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 44.sp
            )
            
            if (reminder != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⏰ ${getRelativeTimeSpan(reminder.date, reminder.time)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // Body Text / Reward Card
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = bodyText,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(pulseScale),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Stars, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                    Column {
                        Text(
                            text = "Potential Reward",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Peace of mind 😌",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Personality Quote & Controls
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "\"$quote\"",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = if (alarmStage == ReminderScheduler.STAGE_DUE_NOW) onStartNow else onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(primaryButtonText.uppercase(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            if (alarmStage == ReminderScheduler.STAGE_DUE_NOW) {
                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onSnooze,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "😅 Give Me 5 Minutes",
                        color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

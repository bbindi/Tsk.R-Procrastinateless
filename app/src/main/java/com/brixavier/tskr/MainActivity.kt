package com.brixavier.tskr

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.*
import com.brixavier.tskr.bloc.ReminderBloc
import com.brixavier.tskr.bloc.ReminderEvent
import com.brixavier.tskr.bloc.ReminderState
import com.brixavier.tskr.data.ReminderDatabase
import com.brixavier.tskr.scheduler.ReminderScheduler
import com.brixavier.tskr.ui.PlanDetailScreen
import com.brixavier.tskr.ui.ReminderDetailScreen
import com.brixavier.tskr.ui.ReminderScreen
import com.brixavier.tskr.ui.Screen
import com.brixavier.tskr.ui.WelcomeScreen
import com.brixavier.tskr.ui.theme.ReminderAppTheme
import com.brixavier.tskr.widget.WidgetUpdateWorker
import android.media.RingtoneManager
import com.brixavier.tskr.personality.ActionType
import com.brixavier.tskr.personality.PersonalityContext
import com.brixavier.tskr.personality.PersonalityEngine
import com.brixavier.tskr.personality.PersonalityIntensity
import com.brixavier.tskr.sound.SharedPreferencesSoundStore
import com.brixavier.tskr.sound.SoundResolver
import com.brixavier.tskr.sound.SoundType
import java.util.concurrent.TimeUnit

/**
 * Main Android Entry point of the application.
 * Hooks up the [ReminderBloc] to the Composable UI [ReminderScreen].
 */
class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_ADD_SHEET = "EXTRA_OPEN_ADD_SHEET"
        const val EXTRA_OPEN_REMINDER_ID = "EXTRA_OPEN_REMINDER_ID"
    }

    private val reminderBloc: ReminderBloc by viewModels {
        ReminderViewModelFactory(
            ReminderDatabase.getDatabase(applicationContext),
            ReminderScheduler(applicationContext),
            applicationContext
        )
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> updatePermissionStates() }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private val isExactAlarmAllowed = mutableStateOf(true)
    private val isBatteryOptimizationIgnored = mutableStateOf(true)

    private var pendingSoundPickerType: SoundType? = null
    private var soundStateVersion by mutableStateOf(0)

    private val soundPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            val type = pendingSoundPickerType ?: return@registerForActivityResult
            val store = SharedPreferencesSoundStore(this)
            if (uri != null) {
                store.setSoundUri(type, uri.toString())
            } else {
                store.resetSoundUri(type)
            }
            soundStateVersion++
        }
    }

    private fun launchSoundPicker(type: SoundType) {
        val store = SharedPreferencesSoundStore(this)
        val currentUri = SoundResolver.resolveSoundUri(this, store, type)
        val pickerType = when (type) {
            SoundType.MISSION_MODE -> RingtoneManager.TYPE_ALARM
            else -> RingtoneManager.TYPE_NOTIFICATION
        }

        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, pickerType)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, when (type) {
                SoundType.NORMAL -> "Choose Default Reminder Sound"
                SoundType.IMPORTANT -> "Choose Important Reminder Sound"
                SoundType.MISSION_MODE -> "Choose Mission Mode Alarm Sound"
            })
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, SoundResolver.getDefaultSystemUri(type))
        }

        pendingSoundPickerType = type
        soundPickerLauncher.launch(intent)
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStates()
    }

    private fun updatePermissionStates() {
        isExactAlarmAllowed.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
        } else {
            true
        }
        isBatteryOptimizationIgnored.value = (getSystemService(POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                packageManager.getPackageInfo(packageName, 0)
            }
            packageInfo.versionName ?: "1.0.1"
        } catch (_: Exception) {
            "1.0.1"
        }
    }

    private fun openBatterySettings() {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS)
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        val sharedPref = getSharedPreferences("tsk_r_prefs", MODE_PRIVATE)
        enqueueWidgetUpdate()
        
        setContent {
            ReminderAppTheme {
                var isFirstRun by remember { 
                    mutableStateOf(sharedPref.getBoolean("isFirstRun", true)) 
                }
                val personalityEngine = remember { PersonalityEngine() }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isFirstRun) {
                        WelcomeScreen(
                            personalityEngine = personalityEngine,
                            onAlarmPermissionRequest = { checkAndRequestExactAlarmPermission() },
                            onNotificationPermissionRequest = { checkAndRequestNotificationsPermission() },
                            onProceed = {
                                sharedPref.edit { putBoolean("isFirstRun", false) }
                                isFirstRun = false
                            }
                        )
                    } else {
                        val state by reminderBloc.state.collectAsStateWithLifecycle()
                        val shouldOpenSheet = intent.getBooleanExtra(EXTRA_OPEN_ADD_SHEET, false)
                        var initialSheetShow by remember { mutableStateOf(shouldOpenSheet) }
                        
                        var selectedReminderId by remember { 
                            val id = intent.getStringExtra(EXTRA_OPEN_REMINDER_ID)
                            if (id != null) {
                                intent.removeExtra(EXTRA_OPEN_REMINDER_ID)
                            }
                            mutableStateOf(id)
                        }

                        var selectedPlanId by remember { mutableStateOf<String?>(null) }
                        var taskPreselectedPlanId by remember { mutableStateOf<String?>(null) }
                        var taskPreselectedReminderId by remember { mutableStateOf<String?>(null) }
                        var activeTab by remember { mutableStateOf(Screen.Home) }

                        val snackbarHostState = remember { SnackbarHostState() }

                        LaunchedEffect(state?.userMessage) {
                            val msg = state?.userMessage
                            val action = state?.userMessageAction

                            if (msg != null) {
                                val fullMessage = if (action != null) {
                                    val streak = state?.streak ?: 0
                                    val isRecurring = state?.uiItems?.find { it.occurrenceId == selectedReminderId || it.templateId == selectedReminderId }?.isRecurring() ?: false

                                    val intensity = when (action) {
                                        ActionType.TASK_CREATED,
                                        ActionType.TASK_RESTORED -> PersonalityIntensity.FRIENDLY
                                        ActionType.COMPLETION -> {
                                            if (streak >= 3) PersonalityIntensity.ACCOUNTABILITY
                                            else PersonalityIntensity.PLAYFUL
                                        }
                                        else -> PersonalityIntensity.PLAYFUL
                                    }

                                    val ctx = PersonalityContext(
                                        actionType = action,
                                        intensity = intensity,
                                        isRecurring = isRecurring,
                                        streak = streak
                                    )
                                    val personalitySuffix = personalityEngine.getMessage(ctx).text
                                    "$msg\n$personalitySuffix"
                                } else {
                                    msg
                                }

                                snackbarHostState.showSnackbar(fullMessage)
                                reminderBloc.onEvent(ReminderEvent.ClearUserMessage)
                            }
                        }

                        LaunchedEffect(intent) {
                            val newId = intent.getStringExtra(EXTRA_OPEN_REMINDER_ID)
                            if (newId != null) {
                                selectedReminderId = newId
                                intent.removeExtra(EXTRA_OPEN_REMINDER_ID)
                            }
                        }

                        // Preferred exact occurrence match -> fallback to template match for legacy intents
                        val selectedUiItem = state?.uiItems?.let { uiItems ->
                            if (selectedReminderId.isNullOrBlank()) {
                                null
                            } else {
                                uiItems.find { it.occurrenceId == selectedReminderId }
                                    ?: uiItems.find { it.templateId == selectedReminderId }
                            }
                        }

                        Scaffold(
                            snackbarHost = {
                                SnackbarHost(
                                    hostState = snackbarHostState,
                                    modifier = Modifier.padding(bottom = 88.dp)
                                )
                            },
                            containerColor = MaterialTheme.colorScheme.background
                        ) { paddingValues ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues)
                            ) {
                                if (selectedUiItem != null) {
                                    ReminderDetailScreen(
                                        item = selectedUiItem,
                                        personalityEngine = personalityEngine,
                                        onEvent = { reminderBloc.onEvent(it) },
                                        onEditTask = { templateId ->
                                            selectedReminderId = null
                                            taskPreselectedReminderId = templateId
                                            initialSheetShow = true
                                        },
                                        onBack = { selectedReminderId = null }
                                    )
                                } else if (selectedPlanId != null) {
                                    PlanDetailScreen(
                                        planId = selectedPlanId!!,
                                        state = state ?: ReminderState(),
                                        personalityEngine = personalityEngine,
                                        onEvent = { reminderBloc.onEvent(it) },
                                        onEditPlan = { plan ->
                                            // Handle edit if needed
                                        },
                                        onCreateTaskForPlan = { planId ->
                                            selectedPlanId = null
                                            taskPreselectedPlanId = planId
                                            initialSheetShow = true
                                        },
                                        onBack = { selectedPlanId = null }
                                    )
                                } else {
                                    ReminderScreen(
                                        state = state,
                                        personalityEngine = personalityEngine,
                                        initialScreen = activeTab,
                                        initialShowBottomSheet = initialSheetShow,
                                        preselectedPlanId = taskPreselectedPlanId,
                                        initialEditingReminderId = taskPreselectedReminderId,
                                        isExactAlarmAllowed = isExactAlarmAllowed.value,
                                        isBatteryOptimizationIgnored = isBatteryOptimizationIgnored.value,
                                        appVersion = getAppVersion(),
                                        onOpenAlarmSettings = { checkAndRequestExactAlarmPermission() },
                                        onOpenBatterySettings = { openBatterySettings() },
                                        onPickSound = { soundType -> launchSoundPicker(soundType) },
                                        onResetSound = { soundType ->
                                            val store = SharedPreferencesSoundStore(this@MainActivity)
                                            store.resetSoundUri(soundType)
                                            soundStateVersion++
                                        },
                                        getSoundTitle = { soundType ->
                                            @Suppress("UNUSED_VARIABLE")
                                            val v = soundStateVersion
                                            val store = SharedPreferencesSoundStore(this@MainActivity)
                                            val storedUri = store.getSoundUri(soundType)
                                            if (storedUri.isNullOrBlank()) {
                                                "System Default"
                                            } else {
                                                SoundResolver.getSoundTitle(this@MainActivity, Uri.parse(storedUri))
                                            }
                                        },
                                        onReminderClick = { reminder -> selectedReminderId = reminder.id },
                                        onPlanClick = { planId -> selectedPlanId = planId },
                                        onTabSelected = { screen -> activeTab = screen },
                                        onEvent = { event ->
                                            reminderBloc.onEvent(event)
                                        },
                                        onSheetClosed = {
                                            initialSheetShow = false
                                            taskPreselectedPlanId = null
                                            taskPreselectedReminderId = null
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

    private fun enqueueWidgetUpdate() {
        val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(1, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WidgetHourlyUpdate",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun checkAndRequestNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun checkAndRequestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
        }
    }
}

class ReminderViewModelFactory(
    private val database: ReminderDatabase,
    private val scheduler: ReminderScheduler,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReminderBloc::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReminderBloc(database.reminderDao(), scheduler, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

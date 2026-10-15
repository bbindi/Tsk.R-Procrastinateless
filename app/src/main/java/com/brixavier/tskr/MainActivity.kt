package com.brixavier.tskr

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import com.brixavier.tskr.update.InAppUpdateManager
import com.brixavier.tskr.update.InAppUpdateStatus
import com.brixavier.tskr.update.PlayInAppUpdateManager
import androidx.compose.foundation.BorderStroke
 import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.brixavier.tskr.ui.theme.TskRMotion
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brixavier.tskr.model.Plan
import com.brixavier.tskr.ui.AddPlanBottomSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.*
import com.brixavier.tskr.ad.AdConsentManager
import com.brixavier.tskr.ad.AdManager
import com.brixavier.tskr.ad.AdMobManager
import com.brixavier.tskr.ad.UmpConsentManager
import com.brixavier.tskr.review.InAppReviewManager
import com.brixavier.tskr.review.PlayInAppReviewManager
import com.brixavier.tskr.bloc.ReminderBloc
import com.brixavier.tskr.bloc.ReminderEvent
import com.brixavier.tskr.bloc.ReminderState
import com.brixavier.tskr.data.ReminderDatabase
import com.brixavier.tskr.model.ReminderOccurrence
import com.brixavier.tskr.personality.ActionType
import com.brixavier.tskr.personality.BehavioralContextResolver
import com.brixavier.tskr.personality.PersonalityContext
import com.brixavier.tskr.personality.PersonalityEngine
import com.brixavier.tskr.personality.PersonalityIntensity
import com.brixavier.tskr.scheduler.ReminderScheduler
import com.brixavier.tskr.sound.SharedPreferencesSoundStore
import com.brixavier.tskr.sound.SoundResolver
import com.brixavier.tskr.sound.SoundType
import com.brixavier.tskr.ui.PlanDetailScreen
import com.brixavier.tskr.ui.ReminderDetailScreen
import com.brixavier.tskr.ui.ReminderScreen
import com.brixavier.tskr.ui.Screen
import com.brixavier.tskr.ui.WelcomeScreen
import com.brixavier.tskr.ui.WhatsNewWalkthrough
import com.brixavier.tskr.ui.theme.AppThemeMode
import com.brixavier.tskr.ui.theme.ReminderAppTheme
import com.brixavier.tskr.widget.WidgetUpdateWorker
import java.time.LocalTime
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

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            Log.d("MainActivity", "In-app update flow canceled or failed (resultCode: ${result.resultCode}).")
        }
    }

    private val inAppUpdateManager: InAppUpdateManager by lazy {
        PlayInAppUpdateManager(this)
    }

    private val adConsentManager: AdConsentManager by lazy {
        UmpConsentManager(this)
    }

    private val adManager: AdManager by lazy {
        AdMobManager(adConsentManager)
    }

    private val inAppReviewManager: InAppReviewManager by lazy {
        PlayInAppReviewManager(this)
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStates()
        inAppUpdateManager.checkForUpdate()
        inAppReviewManager.requestReviewIfEligible(this)
        if (adManager.canRequestAds.value) {
            adManager.initializeSdk(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        inAppUpdateManager.unregisterListener()
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
            packageInfo.versionName ?: BuildConfig.VERSION_NAME
        } catch (_: Exception) {
            BuildConfig.VERSION_NAME
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

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        val sharedPref = getSharedPreferences("tsk_r_prefs", MODE_PRIVATE)
        enqueueWidgetUpdate()
        inAppReviewManager.eligibilityEngine.recordSessionLaunch()
        adManager.gatherConsentAndInitialize(this)
        
        setContent {
            val themeModeStr = sharedPref.getString("theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
            var currentThemeMode by remember {
                mutableStateOf(
                    try {
                        AppThemeMode.valueOf(themeModeStr)
                    } catch (_: Exception) {
                        AppThemeMode.SYSTEM
                    }
                )
            }

            val isSystemDark = isSystemInDarkTheme()
            val isDark = when (currentThemeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> isSystemDark
            }

            ReminderAppTheme(darkTheme = isDark) {
                var isFirstRun by remember { 
                    mutableStateOf(sharedPref.getBoolean("isFirstRun", true)) 
                }
                var v2WhatsNewSeen by remember {
                    mutableStateOf(sharedPref.getBoolean("v2_whats_new_seen", false))
                }
                var showWhatsNewReplay by remember { mutableStateOf(false) }

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
                            onBatteryOptimizationRequest = { openBatterySettings() },
                            onProceed = {
                                sharedPref.edit {
                                    putBoolean("isFirstRun", false)
                                }
                                isFirstRun = false
                            }
                        )
                    } else if (!v2WhatsNewSeen || showWhatsNewReplay) {
                        WhatsNewWalkthrough(
                            isSettingsReplay = showWhatsNewReplay,
                            onComplete = {
                                sharedPref.edit { putBoolean("v2_whats_new_seen", true) }
                                v2WhatsNewSeen = true
                                showWhatsNewReplay = false
                            },
                            onDismiss = {
                                showWhatsNewReplay = false
                            }
                        )
                    } else {
                        val state by reminderBloc.state.collectAsStateWithLifecycle()
                        val updateStatus by inAppUpdateManager.updateStatus.collectAsStateWithLifecycle()
                        val isPrivacyRequired by adManager.isPrivacyOptionsRequired.collectAsStateWithLifecycle()
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
                        var editingPlan by remember { mutableStateOf<Plan?>(null) }
                        var showPlanSheet by remember { mutableStateOf(false) }
                        val planSheetState = rememberModalBottomSheetState()
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

                                    val targetItem = state?.uiItems?.find { it.occurrenceId == selectedReminderId || it.templateId == selectedReminderId }
                                    val isEarly = if (action == ActionType.COMPLETION && targetItem != null) {
                                        targetItem.completedAtMs != null &&
                                        BehavioralContextResolver.isEarlyCompletion(
                                            ReminderOccurrence(
                                                id = targetItem.occurrenceId,
                                                title = targetItem.title,
                                                date = targetItem.date,
                                                time = targetItem.time,
                                                status = targetItem.status,
                                                completedAtMs = targetItem.completedAtMs
                                            )
                                        )
                                    } else false

                                    val ctx = PersonalityContext(
                                        actionType = action,
                                        intensity = intensity,
                                        isRecurring = isRecurring,
                                        streak = streak,
                                        timeOfDayHour = LocalTime.now().hour,
                                        isEarlyCompletion = isEarly
                                    )
                                    val personalitySuffix = personalityEngine.getMessage(ctx).text
                                    "$msg\n$personalitySuffix"
                                } else {
                                    msg
                                }

                                val job = launch {
                                    snackbarHostState.showSnackbar(
                                        message = fullMessage,
                                        duration = SnackbarDuration.Indefinite
                                    )
                                }
                                delay(4500)
                                snackbarHostState.currentSnackbarData?.dismiss()
                                job.cancel()
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
                        val selectedUiItem = remember(selectedReminderId, state?.uiItems) {
                            state?.uiItems?.let { uiItems ->
                                if (selectedReminderId.isNullOrBlank()) {
                                    null
                                } else {
                                    uiItems.find { it.occurrenceId == selectedReminderId }
                                        ?: uiItems.find { it.templateId == selectedReminderId }
                                }
                            }
                        }

                        Scaffold(
                            snackbarHost = {
                                SnackbarHost(
                                    hostState = snackbarHostState,
                                    modifier = Modifier.padding(bottom = 100.dp, start = 16.dp, end = 16.dp)
                                ) { snackbarData ->
                                    val message = snackbarData.visuals.message
                                    val parts = message.split("\n", limit = 2)
                                    val primaryText = parts.getOrNull(0) ?: ""
                                    val personalityText = parts.getOrNull(1)
                                    val density = LocalDensity.current

                                    AnimatedVisibility(
                                        visible = true,
                                        enter = fadeIn(animationSpec = tween(durationMillis = TskRMotion.DURATION_STANDARD, easing = TskRMotion.EasingStandard)) +
                                                slideInVertically(
                                                    animationSpec = tween(durationMillis = TskRMotion.DURATION_STANDARD, easing = TskRMotion.EasingStandard),
                                                    initialOffsetY = { with(density) { 12.dp.roundToPx() } }
                                                ),
                                        exit = fadeOut(animationSpec = tween(durationMillis = TskRMotion.DURATION_MICRO)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            tonalElevation = 6.dp,
                                            shadowElevation = 8.dp,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = primaryText,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (!personalityText.isNullOrBlank()) {
                                                    Text(
                                                        text = personalityText,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            containerColor = Color.Transparent,
                            contentWindowInsets = WindowInsets(0, 0, 0, 0)
                        ) { paddingValues ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues)
                            ) {
                                val density = LocalDensity.current
                                val mainDestinationKey = remember(selectedUiItem, selectedPlanId) {
                                    when {
                                        selectedUiItem != null -> "reminder_detail_${selectedUiItem.occurrenceId}"
                                        selectedPlanId != null -> "plan_detail_${selectedPlanId}"
                                        else -> "main"
                                    }
                                }

                                AnimatedContent(
                                    targetState = mainDestinationKey,
                                    transitionSpec = {
                                        if (targetState == "main") {
                                            TskRMotion.popEnterTransition(density) togetherWith TskRMotion.popExitTransition(density)
                                        } else {
                                            TskRMotion.enterTransition(density) togetherWith TskRMotion.exitTransition(density)
                                        }
                                    },
                                    label = "MainDestinationTransition",
                                    modifier = Modifier.fillMaxSize()
                                ) { targetKey ->
                                    if (targetKey.startsWith("reminder_detail_") && selectedUiItem != null) {
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
                                    } else if (targetKey.startsWith("plan_detail_") && selectedPlanId != null) {
                                        PlanDetailScreen(
                                            planId = selectedPlanId!!,
                                            state = state ?: ReminderState(),
                                            onEvent = { reminderBloc.onEvent(it) },
                                            onEditPlan = { plan ->
                                                editingPlan = plan
                                                showPlanSheet = true
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
                                            adManager = adManager,
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
                                            onOpenWhatsNew = { showWhatsNewReplay = true },
                                            currentThemeMode = currentThemeMode,
                                            isPrivacyOptionsRequired = isPrivacyRequired,
                                            onOpenPrivacyOptions = { adManager.showPrivacyOptionsForm(this@MainActivity) },
                                            onRateApp = { inAppReviewManager.launchPlayStoreListing(this@MainActivity) },
                                            onThemeModeChange = { newMode ->
                                                sharedPref.edit { putString("theme_mode", newMode.name) }
                                                currentThemeMode = newMode
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

                                if (updateStatus is InAppUpdateStatus.UpdateAvailable) {
                                    Snackbar(
                                        action = {
                                            TextButton(onClick = {
                                                inAppUpdateManager.startFlexibleUpdate(this@MainActivity, updateLauncher)
                                            }) {
                                                Text("UPDATE", fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        dismissAction = {
                                            IconButton(onClick = { inAppUpdateManager.dismissUpdatePrompt() }) {
                                                Icon(Icons.Default.Close, contentDescription = "Dismiss")
                                            }
                                        },
                                        modifier = Modifier
                                            .statusBarsPadding()
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                            .align(Alignment.TopCenter)
                                    ) {
                                        Text("A new version of Tsk.R is available.")
                                    }
                                } else if (updateStatus is InAppUpdateStatus.Downloaded) {
                                    Snackbar(
                                        action = {
                                            TextButton(onClick = { inAppUpdateManager.completeUpdate() }) {
                                                Text("RESTART", fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        modifier = Modifier
                                            .statusBarsPadding()
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                            .align(Alignment.TopCenter)
                                    ) {
                                        Text("Tsk.R has been updated and is ready to restart.")
                                    }
                                }
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
                                            reminderBloc.onEvent(
                                                ReminderEvent.UpdatePlan(
                                                    editPln.copy(
                                                        name = name,
                                                        description = description,
                                                        icon = icon,
                                                        deadline = deadline,
                                                        category = category,
                                                        categorySource = categorySource,
                                                        color = color,
                                                        colorSource = colorSource
                                                    )
                                                )
                                            )
                                        } else {
                                            reminderBloc.onEvent(
                                                ReminderEvent.AddPlan(
                                                    name = name,
                                                    description = description,
                                                    icon = icon,
                                                    deadline = deadline,
                                                    category = category,
                                                    categorySource = categorySource,
                                                    color = color,
                                                    colorSource = colorSource
                                                )
                                            )
                                        }
                                        showPlanSheet = false
                                        editingPlan = null
                                    }
                                )
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

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.*
import com.brixavier.tskr.bloc.ReminderBloc
import com.brixavier.tskr.data.ReminderDatabase
import com.brixavier.tskr.scheduler.ReminderScheduler
import com.brixavier.tskr.ui.ReminderDetailScreen
import com.brixavier.tskr.ui.ReminderScreen
import com.brixavier.tskr.ui.WelcomeScreen
import com.brixavier.tskr.ui.theme.ReminderAppTheme
import com.brixavier.tskr.widget.WidgetUpdateWorker
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

    // Retrieve our ViewModel instance which acts as our BLoC using the custom Factory
    private val reminderBloc: ReminderBloc by viewModels {
        ReminderViewModelFactory(
            ReminderDatabase.getDatabase(applicationContext),
            ReminderScheduler(applicationContext),
            applicationContext
        )
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private val isExactAlarmAllowed = mutableStateOf(true)
    private val isBatteryOptimizationIgnored = mutableStateOf(true)

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

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isFirstRun) {
                        WelcomeScreen(
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
                            mutableStateOf(intent.getStringExtra(EXTRA_OPEN_REMINDER_ID)) 
                        }

                        LaunchedEffect(intent) {
                            val newId = intent.getStringExtra(EXTRA_OPEN_REMINDER_ID)
                            if (newId != null) {
                                selectedReminderId = newId
                            }
                        }

                        val selectedReminder = state?.reminders?.find { it.id == selectedReminderId }

                        if (selectedReminder != null) {
                            ReminderDetailScreen(
                                reminder = selectedReminder,
                                onEvent = { reminderBloc.onEvent(it) },
                                onBack = { selectedReminderId = null }
                            )
                        } else {
                            ReminderScreen(
                                state = state,
                                initialShowBottomSheet = initialSheetShow,
                                isExactAlarmAllowed = isExactAlarmAllowed.value,
                                isBatteryOptimizationIgnored = isBatteryOptimizationIgnored.value,
                                appVersion = getAppVersion(),
                                onOpenAlarmSettings = { checkAndRequestExactAlarmPermission() },
                                onOpenBatterySettings = { openBatterySettings() },
                                onReminderClick = { selectedReminderId = it.id },
                                onEvent = { event ->
                                    reminderBloc.onEvent(event)
                                },
                                onSheetClosed = { initialSheetShow = false }
                            )
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

/**
 * Factory class to instantiate the ReminderBloc with dependencies.
 */
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

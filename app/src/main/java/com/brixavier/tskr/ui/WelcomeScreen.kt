package com.brixavier.tskr.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.brixavier.tskr.R
import com.brixavier.tskr.personality.PersonalityEngine
import com.brixavier.tskr.personality.PersonalityContext
import com.brixavier.tskr.personality.ActionType

private fun checkExactAlarmPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        alarmManager?.canScheduleExactAlarms() ?: true
    } else {
        true
    }
}

private fun checkNotificationPermission(context: Context): Boolean {
    val isPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    return isPermissionGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
}

private fun checkBatteryOptimizationPermission(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
}

@Composable
fun WelcomeScreen(
    personalityEngine: PersonalityEngine,
    onAlarmPermissionRequest: () -> Unit,
    onNotificationPermissionRequest: () -> Unit,
    onBatteryOptimizationRequest: () -> Unit = {},
    onProceed: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isAlarmGranted by remember {
        mutableStateOf(checkExactAlarmPermission(context))
    }
    var isNotificationGranted by remember {
        mutableStateOf(checkNotificationPermission(context))
    }
    var isBatteryGranted by remember {
        mutableStateOf(checkBatteryOptimizationPermission(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAlarmGranted = checkExactAlarmPermission(context)
                isNotificationGranted = checkNotificationPermission(context)
                isBatteryGranted = checkBatteryOptimizationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showDemands by remember { mutableStateOf(false) }

    val onboardingMessage = remember {
        personalityEngine.getMessage(PersonalityContext(actionType = ActionType.ONBOARDING)).text
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        AnimatedContent(
            targetState = showDemands,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "OnboardingTransition"
        ) { isDemands ->
            if (!isDemands) {
                WelcomeView(
                    funnyLine = onboardingMessage,
                    onEnter = { showDemands = true }
                )
            } else {
                DemandsView(
                    isAlarmGranted = isAlarmGranted,
                    isNotificationGranted = isNotificationGranted,
                    isBatteryGranted = isBatteryGranted,
                    onAlarmRequest = onAlarmPermissionRequest,
                    onNotificationRequest = onNotificationPermissionRequest,
                    onBatteryRequest = onBatteryOptimizationRequest,
                    onProceed = onProceed
                )
            }
        }
    }
}

@Composable
fun WelcomeView(
    funnyLine: String,
    onEnter: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.app_full_name),
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "(${stringResource(R.string.app_name)} = ${stringResource(R.string.app_meaning)})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(
                topStart = 32.dp,
                topEnd = 8.dp,
                bottomStart = 8.dp,
                bottomEnd = 32.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Text(
                text = funnyLine,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(24.dp),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onEnter,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = "ENTER THE CHASM",
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun DemandsView(
    isAlarmGranted: Boolean,
    isNotificationGranted: Boolean,
    isBatteryGranted: Boolean,
    onAlarmRequest: () -> Unit,
    onNotificationRequest: () -> Unit,
    onBatteryRequest: () -> Unit,
    onProceed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "The Mandatory Demands",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        PermissionCard(
            icon = Icons.Default.Alarm,
            title = "Alarms & Exact Scheduling",
            description = "Required to trigger our Mission Mode alarm loop.",
            buttonText = "Grant Alarm Access",
            grantedStatusText = "✓ Alarm access enabled",
            isGranted = isAlarmGranted,
            onGrant = onAlarmRequest
        )

        PermissionCard(
            icon = Icons.Default.Notifications,
            title = "Notifications",
            description = "Required for daily encouragement and sarcastic alerts.",
            buttonText = "Grant Notifications",
            grantedStatusText = "✓ Notifications enabled",
            isGranted = isNotificationGranted,
            onGrant = onNotificationRequest
        )

        PermissionCard(
            icon = Icons.Default.BatteryChargingFull,
            title = "Background Activity (Recommended)",
            description = "Android may put Tsk.R to sleep to save battery, which can delay your alarms and reminders.",
            buttonText = "Allow Background Activity",
            grantedStatusText = "✓ Background activity allowed",
            isGranted = isBatteryGranted,
            onGrant = onBatteryRequest
        )

        Spacer(modifier = Modifier.weight(1f, fill = false))

        Button(
            onClick = onProceed,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Text(
                text = "PROCEED TO DASHBOARD",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    buttonText: String,
    grantedStatusText: String,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isGranted) {
                Text(
                    text = grantedStatusText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                FilledTonalButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Configured")
                }
            } else {
                FilledTonalButton(
                    onClick = onGrant,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(buttonText)
                }
            }
        }
    }
}

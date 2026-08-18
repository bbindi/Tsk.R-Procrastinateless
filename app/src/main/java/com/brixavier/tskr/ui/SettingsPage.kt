package com.brixavier.tskr.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsPage(
    isExactAlarmAllowed: Boolean,
    isBatteryOptimizationIgnored: Boolean,
    appVersion: String,
    onOpenAlarmSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        SettingsSection(title = "STAYING ON TRACK") {
            SettingsItem(
                icon = Icons.Default.Alarm,
                title = "Punctuality Protocol",
                description = "Ensures I can annoy you exactly when I'm supposed to. Without this, I might be late, and we both know you'll use that as an excuse.",
                statusText = if (isExactAlarmAllowed) "✓ Ready to pounce" else "⚠ Timing compromised",
                statusColor = if (isExactAlarmAllowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                actionText = if (isExactAlarmAllowed) "Configure" else "Fix My Timing",
                isActionRequired = !isExactAlarmAllowed,
                onAction = onOpenAlarmSettings
            )

            SettingsItem(
                icon = Icons.Default.BatteryChargingFull,
                title = "Energy vs. Efficiency",
                description = "Android likes putting apps to sleep to save battery. Unfortunately, sleeping is not part of my job description.",
                statusText = if (isBatteryOptimizationIgnored) "✓ Always watching" else "⚠ Android is suppressing me",
                statusColor = if (isBatteryOptimizationIgnored) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                actionText = "Allow Background Activity",
                isActionRequired = !isBatteryOptimizationIgnored,
                onAction = onOpenBatterySettings
            )
        }

        SettingsSection(title = "THE BITS AND BYTES") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "Tsk.R",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Version $appVersion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "The No More Excuses Edition",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
        )
        content()
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    description: String,
    statusText: String,
    statusColor: androidx.compose.ui.graphics.Color,
    actionText: String,
    isActionRequired: Boolean = false,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
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

            if (isActionRequired) {
                // Action Needed: Stack vertically for prominence and responsiveness
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelLarge,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )

                    FilledTonalButton(
                        onClick = onAction,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(actionText, style = MaterialTheme.typography.labelLarge)
                    }
                }
            } else {
                // Configured Correctly: Keep compact horizontal layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelLarge,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(onClick = onAction) {
                        Text(actionText)
                    }
                }
            }
        }
    }
}

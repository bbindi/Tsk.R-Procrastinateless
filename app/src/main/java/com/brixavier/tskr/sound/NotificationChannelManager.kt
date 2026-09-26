package com.brixavier.tskr.sound

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.brixavier.tskr.scheduler.ReminderScheduler
import java.security.MessageDigest

/**
 * Authoritative helper to calculate stage-aware notification IDs deterministically.
 */
object NotificationIdHelper {
    fun getNotificationId(occurrenceId: String, stage: String): Int {
        val hash = occurrenceId.hashCode() and 0x00ffffff
        val offset = when (stage) {
            ReminderScheduler.STAGE_7_DAYS -> 1
            ReminderScheduler.STAGE_24_HOURS -> 2
            ReminderScheduler.STAGE_DUE_NOW -> 3
            else -> 9
        }
        return hash * 10 + offset
    }
}

/**
 * Reusable notification-channel manager for Tsk.R that dynamically handles sound-specific channels.
 *
 * Android O+ (API 26+) notification channels are immutable after creation.
 * To support dynamic sound customization, Tsk.R generates deterministic channel IDs based on category
 * and sound URI hash (e.g. `tsk_r_channel_normal_<hash>`).
 */
object NotificationChannelManager {

    const val LEGACY_CHANNEL_ID = "tsk_r_mission_critical_v3"
    private const val CHANNEL_PREFIX = "tsk_r_channel_"

    /**
     * Generates a safe, deterministic channel ID from category and sound URI.
     */
    fun generateChannelId(category: String, uriString: String): String {
        val input = "$category|$uriString"
        val hash = try {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            bytes.take(6).joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            (input.hashCode() and 0x7fffffff).toString(16)
        }
        return "${CHANNEL_PREFIX}${category}_$hash"
    }

    /**
     * Retrieves or creates a sound-specific NotificationChannel for the given SoundType.
     * Reuses existing channel if the sound URI matches, or creates a new dynamic channel if sound changed.
     * Automatically cleans up obsolete Tsk.R dynamic channels for the same category.
     */
    fun getOrCreateNotificationChannel(
        context: Context,
        soundType: SoundType,
        store: SoundPreferenceStore = SharedPreferencesSoundStore(context)
    ): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return LEGACY_CHANNEL_ID
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val resolvedUri = SoundResolver.resolveSoundUri(context, store, soundType)
        val categoryPrefix = when (soundType) {
            SoundType.IMPORTANT -> "important"
            else -> "normal"
        }

        val channelId = generateChannelId(categoryPrefix, resolvedUri.toString())
        val channelName = when (soundType) {
            SoundType.IMPORTANT -> "Important Mission Alerts"
            else -> "Default Reminder Alerts"
        }

        // Create channel if it doesn't already exist
        val existingChannel = notificationManager.getNotificationChannel(channelId)
        if (existingChannel == null) {
            val audioUsage = when (soundType) {
                SoundType.IMPORTANT -> AudioAttributes.USAGE_ALARM
                else -> AudioAttributes.USAGE_NOTIFICATION
            }

            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for Tsk.R $categoryPrefix alerts"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setBypassDnd(soundType == SoundType.IMPORTANT)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setSound(
                    resolvedUri,
                    AudioAttributes.Builder()
                        .setUsage(audioUsage)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Clean up obsolete dynamic Tsk.R channels for this category
        cleanupObsoleteChannels(notificationManager, categoryPrefix, activeChannelId = channelId)

        return channelId
    }

    /**
     * Safely deletes obsolete dynamic Tsk.R notification channels for a category that are no longer active.
     */
    private fun cleanupObsoleteChannels(
        notificationManager: NotificationManager,
        categoryPrefix: String,
        activeChannelId: String
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        try {
            val targetPrefix = "${CHANNEL_PREFIX}${categoryPrefix}_"
            val channels = notificationManager.notificationChannels ?: return
            for (channel in channels) {
                if (channel.id.startsWith(targetPrefix) && channel.id != activeChannelId) {
                    notificationManager.deleteNotificationChannel(channel.id)
                }
            }
        } catch (_: Exception) {
            // Ignore channel cleanup errors on vendor-modified ROMs
        }
    }
}

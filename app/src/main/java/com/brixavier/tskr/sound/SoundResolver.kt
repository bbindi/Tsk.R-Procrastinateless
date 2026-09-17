package com.brixavier.tskr.sound

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri

object SoundResolver {

    /**
     * Resolves default system Uri for the specified SoundType.
     */
    fun getDefaultSystemUri(type: SoundType): Uri {
        return when (type) {
            SoundType.NORMAL, SoundType.IMPORTANT -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            SoundType.MISSION_MODE -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }
    }

    /**
     * Resolves sound Uri from stored preferences with fallback to default system Uri.
     */
    fun resolveSoundUri(context: Context, store: SoundPreferenceStore, type: SoundType): Uri {
        val storedUriString = store.getSoundUri(type)
        if (!storedUriString.isNullOrBlank()) {
            try {
                val parsedUri = Uri.parse(storedUriString)
                if (isUriResolvable(context, parsedUri)) {
                    return parsedUri
                }
            } catch (_: Exception) {
                // Fallback to default
            }
        }
        return getDefaultSystemUri(type)
    }

    /**
     * Resolves human-readable title for a sound Uri or fallback.
     */
    fun getSoundTitle(context: Context, uri: Uri?): String {
        if (uri == null) return "System Default"
        return try {
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.getTitle(context) ?: "Custom Sound"
        } catch (_: Exception) {
            "Custom Sound"
        }
    }

    private fun isUriResolvable(context: Context, uri: Uri): Boolean {
        return try {
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone != null
        } catch (_: Exception) {
            false
        }
    }
}

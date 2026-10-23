package com.brixavier.tskr.sound

import android.content.Context
import android.content.SharedPreferences

enum class SoundType {
    NORMAL,
    IMPORTANT,
    MISSION_MODE
}

interface SoundPreferenceStore {
    fun getSoundUri(type: SoundType): String?
    fun setSoundUri(type: SoundType, uriString: String?)
    fun resetSoundUri(type: SoundType)
    fun resetAllSounds()
}

class SharedPreferencesSoundStore(context: Context) : SoundPreferenceStore {
    private val prefs: SharedPreferences = context.getSharedPreferences("tsk_r_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_SOUND_NORMAL_URI = "key_sound_normal_uri"
        const val KEY_SOUND_IMPORTANT_URI = "key_sound_important_uri"
        const val KEY_SOUND_MISSION_URI = "key_sound_mission_uri"
    }

    override fun getSoundUri(type: SoundType): String? {
        val key = getKey(type)
        val storedUri = prefs.getString(key, null)
        // Older versions kept a separate Mission Mode sound. Reuse it as the
        // Important alarm sound when no Important sound has been chosen yet.
        return if (type == SoundType.IMPORTANT && storedUri.isNullOrBlank()) {
            prefs.getString(KEY_SOUND_MISSION_URI, null) ?: storedUri
        } else {
            storedUri
        }
    }

    override fun setSoundUri(type: SoundType, uriString: String?) {
        val key = getKey(type)
        prefs.edit().apply {
            if (uriString.isNullOrBlank()) {
                remove(key)
            } else {
                putString(key, uriString)
            }
            if (type == SoundType.IMPORTANT) remove(KEY_SOUND_MISSION_URI)
        }.apply()
    }

    override fun resetSoundUri(type: SoundType) {
        setSoundUri(type, null)
    }

    override fun resetAllSounds() {
        prefs.edit()
            .remove(KEY_SOUND_NORMAL_URI)
            .remove(KEY_SOUND_IMPORTANT_URI)
            .remove(KEY_SOUND_MISSION_URI)
            .apply()
    }

    private fun getKey(type: SoundType): String = when (type) {
        SoundType.NORMAL -> KEY_SOUND_NORMAL_URI
        SoundType.IMPORTANT -> KEY_SOUND_IMPORTANT_URI
        SoundType.MISSION_MODE -> KEY_SOUND_MISSION_URI
    }
}

package com.brixavier.tskr.sound

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FakeSoundPreferenceStore : SoundPreferenceStore {
    private val memoryStore = mutableMapOf<SoundType, String>()

    override fun getSoundUri(type: SoundType): String? {
        return memoryStore[type]
    }

    override fun setSoundUri(type: SoundType, uriString: String?) {
        if (uriString.isNullOrBlank()) {
            memoryStore.remove(type)
        } else {
            memoryStore[type] = uriString
        }
    }

    override fun resetSoundUri(type: SoundType) {
        setSoundUri(type, null)
    }

    override fun resetAllSounds() {
        memoryStore.clear()
    }
}

class SoundPreferencesTest {

    private lateinit var store: SoundPreferenceStore

    @Before
    fun setUp() {
        store = FakeSoundPreferenceStore()
    }

    @Test
    fun testMissingPreference_returnsNull() {
        assertNull(store.getSoundUri(SoundType.NORMAL))
        assertNull(store.getSoundUri(SoundType.IMPORTANT))
        assertNull(store.getSoundUri(SoundType.MISSION_MODE))
    }

    @Test
    fun testSaveAndRetrieveNormalSoundUri() {
        val testUri = "content://settings/system/notification_sound"
        store.setSoundUri(SoundType.NORMAL, testUri)
        assertEquals(testUri, store.getSoundUri(SoundType.NORMAL))
    }

    @Test
    fun testSaveAndRetrieveImportantSoundUri() {
        val testUri = "content://media/internal/audio/media/100"
        store.setSoundUri(SoundType.IMPORTANT, testUri)
        assertEquals(testUri, store.getSoundUri(SoundType.IMPORTANT))
    }

    @Test
    fun testSaveAndRetrieveMissionModeSoundUri() {
        val testUri = "content://media/internal/audio/media/200"
        store.setSoundUri(SoundType.MISSION_MODE, testUri)
        assertEquals(testUri, store.getSoundUri(SoundType.MISSION_MODE))
    }

    @Test
    fun testResetSoundUri_clearsSpecificSoundPreference() {
        store.setSoundUri(SoundType.NORMAL, "content://test/1")
        store.setSoundUri(SoundType.IMPORTANT, "content://test/2")

        store.resetSoundUri(SoundType.NORMAL)

        assertNull(store.getSoundUri(SoundType.NORMAL))
        assertEquals("content://test/2", store.getSoundUri(SoundType.IMPORTANT))
    }

    @Test
    fun testResetAllSounds_clearsAllSoundPreferences() {
        store.setSoundUri(SoundType.NORMAL, "content://test/1")
        store.setSoundUri(SoundType.IMPORTANT, "content://test/2")
        store.setSoundUri(SoundType.MISSION_MODE, "content://test/3")

        store.resetAllSounds()

        assertNull(store.getSoundUri(SoundType.NORMAL))
        assertNull(store.getSoundUri(SoundType.IMPORTANT))
        assertNull(store.getSoundUri(SoundType.MISSION_MODE))
    }

    @Test
    fun testMissionModeSound_remainsIndependentOfNormalAndImportantSounds() {
        store.setSoundUri(SoundType.NORMAL, "content://test/normal")
        store.setSoundUri(SoundType.IMPORTANT, "content://test/important")
        store.setSoundUri(SoundType.MISSION_MODE, "content://test/mission")

        assertEquals("content://test/normal", store.getSoundUri(SoundType.NORMAL))
        assertEquals("content://test/important", store.getSoundUri(SoundType.IMPORTANT))
        assertEquals("content://test/mission", store.getSoundUri(SoundType.MISSION_MODE))

        store.setSoundUri(SoundType.MISSION_MODE, "content://test/mission_updated")
        assertEquals("content://test/normal", store.getSoundUri(SoundType.NORMAL))
        assertEquals("content://test/important", store.getSoundUri(SoundType.IMPORTANT))
        assertEquals("content://test/mission_updated", store.getSoundUri(SoundType.MISSION_MODE))
    }
}

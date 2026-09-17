package com.brixavier.tskr.sound

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationChannelManagerTest {

    @Test
    fun testGenerateChannelId_sameNormalUri_producesSameChannelId() {
        val uri = "content://settings/system/notification_sound"
        val id1 = NotificationChannelManager.generateChannelId("normal", uri)
        val id2 = NotificationChannelManager.generateChannelId("normal", uri)

        assertEquals(id1, id2)
        assertTrue(id1.startsWith("tsk_r_channel_normal_"))
    }

    @Test
    fun testGenerateChannelId_sameImportantUri_producesSameChannelId() {
        val uri = "content://media/internal/audio/media/100"
        val id1 = NotificationChannelManager.generateChannelId("important", uri)
        val id2 = NotificationChannelManager.generateChannelId("important", uri)

        assertEquals(id1, id2)
        assertTrue(id1.startsWith("tsk_r_channel_important_"))
    }

    @Test
    fun testGenerateChannelId_normalAndImportantWithSameUri_produceDifferentIds() {
        val uri = "content://settings/system/notification_sound"
        val normalId = NotificationChannelManager.generateChannelId("normal", uri)
        val importantId = NotificationChannelManager.generateChannelId("important", uri)

        assertNotEquals(normalId, importantId)
        assertTrue(normalId.startsWith("tsk_r_channel_normal_"))
        assertTrue(importantId.startsWith("tsk_r_channel_important_"))
    }

    @Test
    fun testGenerateChannelId_differentUris_produceDifferentIds() {
        val uri1 = "content://settings/system/notification_sound"
        val uri2 = "content://media/internal/audio/media/999"

        val id1 = NotificationChannelManager.generateChannelId("normal", uri1)
        val id2 = NotificationChannelManager.generateChannelId("normal", uri2)

        assertNotEquals(id1, id2)
    }

    @Test
    fun testLegacyChannelId_differsFromDynamicChannelId() {
        val dynamicId = NotificationChannelManager.generateChannelId("normal", "content://test/1")
        assertNotEquals(NotificationChannelManager.LEGACY_CHANNEL_ID, dynamicId)
        assertEquals("tsk_r_mission_critical_v3", NotificationChannelManager.LEGACY_CHANNEL_ID)
    }

    @Test
    fun testGenerateChannelId_containsOnlySafeCharacters() {
        val uri = "content://settings/system/notification_sound?id=123&test=true#fragment"
        val channelId = NotificationChannelManager.generateChannelId("normal", uri)

        // Must contain only alphanumeric, underscores, or hyphens
        assertTrue(channelId.matches(Regex("^[a-zA-Z0-9_-]+$")))
    }
}

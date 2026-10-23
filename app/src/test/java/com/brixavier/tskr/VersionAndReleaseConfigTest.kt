package com.brixavier.tskr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionAndReleaseConfigTest {

    @Test
    fun testVersionName_isTwoOneOne() {
        assertEquals("2.1.1", BuildConfig.VERSION_NAME)
    }

    @Test
    fun testVersionCode_isEightForInternalTesting() {
        assertTrue(BuildConfig.VERSION_CODE >= 8)
        assertEquals(8, BuildConfig.VERSION_CODE)
    }

    @Test
    fun testApplicationId_isCorrect() {
        assertEquals("com.brixavier.tskr", BuildConfig.APPLICATION_ID)
    }
}

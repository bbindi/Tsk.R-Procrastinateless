package com.brixavier.tskr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionAndReleaseConfigTest {

    @Test
    fun testVersionName_isTwoOneZero() {
        assertEquals("2.1.0", BuildConfig.VERSION_NAME)
    }

    @Test
    fun testVersionCode_isSevenForInternalTesting() {
        assertTrue(BuildConfig.VERSION_CODE >= 7)
        assertEquals(7, BuildConfig.VERSION_CODE)
    }

    @Test
    fun testApplicationId_isCorrect() {
        assertEquals("com.brixavier.tskr", BuildConfig.APPLICATION_ID)
    }
}

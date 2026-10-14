package com.brixavier.tskr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionAndReleaseConfigTest {

    @Test
    fun testVersionName_isTwoZeroZero() {
        assertEquals("2.0.0", BuildConfig.VERSION_NAME)
    }

    @Test
    fun testVersionCode_isGreaterThanFive() {
        assertTrue(BuildConfig.VERSION_CODE > 5)
        assertEquals(6, BuildConfig.VERSION_CODE)
    }

    @Test
    fun testApplicationId_isCorrect() {
        assertEquals("com.brixavier.tskr", BuildConfig.APPLICATION_ID)
    }
}

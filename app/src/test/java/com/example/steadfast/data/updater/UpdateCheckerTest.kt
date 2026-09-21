package com.example.steadfast.data.updater

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun isNewerVersion_higherMinor_returnsTrue() {
        assertTrue(DefaultUpdateChecker.isNewerVersion("0.3.0", "0.2.0"))
        assertTrue(DefaultUpdateChecker.isNewerVersion("v0.3.0", "0.2.0"))
        assertTrue(DefaultUpdateChecker.isNewerVersion("0.3.0", "v0.2.0"))
        assertTrue(DefaultUpdateChecker.isNewerVersion("v0.3.0-rc1", "0.2.0"))
        assertTrue(DefaultUpdateChecker.isNewerVersion("0.4.0", "0.3.0"))
        assertTrue(DefaultUpdateChecker.isNewerVersion("v0.4.0", "0.3.0"))
        assertTrue(DefaultUpdateChecker.isNewerVersion("0.5.0", "0.4.0"))
    }

    @Test
    fun isNewerVersion_higherPatch_returnsTrue() {
        assertTrue(DefaultUpdateChecker.isNewerVersion("0.4.1", "0.4.0"))
        assertTrue(DefaultUpdateChecker.isNewerVersion("0.3.1", "0.3.0"))
    }

    @Test
    fun isNewerVersion_higherMajor_returnsTrue() {
        assertTrue(DefaultUpdateChecker.isNewerVersion("1.0.0", "0.4.0"))
    }

    @Test
    fun isNewerVersion_sameVersion_returnsFalse() {
        assertFalse(DefaultUpdateChecker.isNewerVersion("0.4.0", "0.4.0"))
        assertFalse(DefaultUpdateChecker.isNewerVersion("v0.4.0", "0.4.0"))
        assertFalse(DefaultUpdateChecker.isNewerVersion("0.4.0", "v0.4.0"))
        assertFalse(DefaultUpdateChecker.isNewerVersion("0.4.0-beta", "0.4.0"))
    }

    @Test
    fun isNewerVersion_olderVersion_returnsFalse() {
        assertFalse(DefaultUpdateChecker.isNewerVersion("0.3.0", "0.4.0"))
        assertFalse(DefaultUpdateChecker.isNewerVersion("0.2.0", "0.4.0"))
        assertFalse(DefaultUpdateChecker.isNewerVersion("0.1.0", "0.4.0"))
    }
}

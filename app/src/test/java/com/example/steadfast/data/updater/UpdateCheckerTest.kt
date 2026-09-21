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
        assertTrue(DefaultUpdateChecker.isNewerVersion("v0.5.0", "0.4.0"))
        assertTrue(DefaultUpdateChecker.isNewerVersion("0.6.0", "0.5.0"))
    }

    @Test
    fun isNewerVersion_higherPatch_returnsTrue() {
        assertTrue(DefaultUpdateChecker.isNewerVersion("0.5.1", "0.5.0"))
        assertTrue(DefaultUpdateChecker.isNewerVersion("0.4.1", "0.4.0"))
    }

    @Test
    fun isNewerVersion_higherMajor_returnsTrue() {
        assertTrue(DefaultUpdateChecker.isNewerVersion("1.0.0", "0.5.0"))
    }

    @Test
    fun isNewerVersion_sameVersion_returnsFalse() {
        assertFalse(DefaultUpdateChecker.isNewerVersion("0.5.0", "0.5.0"))
        assertFalse(DefaultUpdateChecker.isNewerVersion("v0.5.0", "0.5.0"))
        assertFalse(DefaultUpdateChecker.isNewerVersion("0.5.0-beta", "0.5.0"))
    }

    @Test
    fun isNewerVersion_olderVersion_returnsFalse() {
        assertFalse(DefaultUpdateChecker.isNewerVersion("0.4.0", "0.5.0"))
        assertFalse(DefaultUpdateChecker.isNewerVersion("0.3.0", "0.5.0"))
        assertFalse(DefaultUpdateChecker.isNewerVersion("0.1.0", "0.5.0"))
    }

    @Test
    fun findReleaseApkUrl_prioritizesReleaseOverDebugApk() {
        val assets = listOf(
            ReleaseAsset("steadfast-v0.5.0-debug.apk", "https://github.com/download/debug.apk"),
            ReleaseAsset("steadfast-v0.5.0-release.apk", "https://github.com/download/release.apk"),
            ReleaseAsset("checksums.txt", "https://github.com/download/checksums.txt")
        )

        val url = DefaultUpdateChecker.findReleaseApkUrl(assets, "https://github.com/fallback")
        org.junit.Assert.assertEquals("https://github.com/download/release.apk", url)
    }

    @Test
    fun findReleaseApkUrl_genericApk_selectedWhenNoReleaseKeyword() {
        val assets = listOf(
            ReleaseAsset("steadfast-v0.5.0-debug.apk", "https://github.com/download/debug.apk"),
            ReleaseAsset("steadfast.apk", "https://github.com/download/steadfast.apk")
        )

        val url = DefaultUpdateChecker.findReleaseApkUrl(assets, "https://github.com/fallback")
        org.junit.Assert.assertEquals("https://github.com/download/steadfast.apk", url)
    }

    @Test
    fun findReleaseApkUrl_onlyDebugApk_fallsBackToPageUrl() {
        val assets = listOf(
            ReleaseAsset("steadfast-debug.apk", "https://github.com/download/debug.apk")
        )

        val url = DefaultUpdateChecker.findReleaseApkUrl(assets, "https://github.com/fallback")
        org.junit.Assert.assertEquals("https://github.com/fallback", url)
    }

    @Test
    fun findReleaseApkUrl_nonApkAssets_ignored() {
        val assets = listOf(
            ReleaseAsset("steadfast.aab", "https://github.com/download/steadfast.aab"),
            ReleaseAsset("checksums.txt", "https://github.com/download/checksums.txt")
        )

        val url = DefaultUpdateChecker.findReleaseApkUrl(assets, "https://github.com/fallback")
        org.junit.Assert.assertEquals("https://github.com/fallback", url)
    }
}

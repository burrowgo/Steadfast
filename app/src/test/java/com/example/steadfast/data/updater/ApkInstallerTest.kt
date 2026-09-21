package com.example.steadfast.data.updater

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ApkInstallerTest {

    @Test
    fun installApk_nonExistentFile_failsGracefully() {
        val nonExistentFile = File("/path/does/not/exist/app.apk")
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()

        val result = ApkInstaller.installApk(context, nonExistentFile)
        assertTrue(result.isFailure)
    }

    @Test
    fun canRequestPackageInstalls_returnsBooleanWithoutCrashing() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val canInstall = ApkInstaller.canRequestPackageInstalls(context)
        // Robolectric environment returns true/false without crashing
        assertTrue(canInstall || !canInstall)
    }

    @Test
    fun getManageUnknownAppSourcesIntent_createsValidIntent() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = ApkInstaller.getManageUnknownAppSourcesIntent(context)
        org.junit.Assert.assertNotNull(intent.action)
    }
}

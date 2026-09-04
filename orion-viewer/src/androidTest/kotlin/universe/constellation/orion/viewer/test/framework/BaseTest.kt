package universe.constellation.orion.viewer.test.framework

import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestName
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.djvu.DjvuDocument
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.log
import java.io.File

internal const val MANUAL_DEBUG = false
internal const val LONG_TIMEOUT: Long = 10000
internal const val SHORT_TIMEOUT: Long = 1000

abstract class BaseTest {

    @JvmField
    @Rule
    val name = TestName()

    @Rule
    @JvmField
    val mRuntimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )


    @Before
    fun grantManageExternalStorage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()) return
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val packageName = instrumentation.targetContext.packageName
        val fd = instrumentation.uiAutomation
            .executeShellCommand("appops set $packageName MANAGE_EXTERNAL_STORAGE allow")
        ParcelFileDescriptor.AutoCloseInputStream(fd).use { it.readBytes() }
        val deadline = System.currentTimeMillis() + SHORT_TIMEOUT
        while (!Environment.isExternalStorageManager() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
        }
        assertTrue("Failed to grant MANAGE_EXTERNAL_STORAGE via appops", Environment.isExternalStorageManager())
    }

    @Before
    fun testStart() {
        log("Starting test: ${name.methodName}" )
    }

    @After
    fun testEnd() {
        log("Finishing test: ${name.methodName}" )
    }

    fun dumpBitmap(suffix: String, bitmap: Bitmap) {
        dumpBitmap(name.methodName, suffix, bitmap)
    }

    companion object {
        val testFolder: File = File(Environment.getExternalStorageDirectory(), "Download/orion")
        val testDataFolder: File = File(testFolder, "testData")
        val testFailures: File = File(testFolder, "failures")

        const val SICP: String = "sicp.pdf"

        const val ALICE: String = "aliceinw.djvu"

        const val DJVU_SPEC: String = "DjVu3Spec.djvu"

        const val MD_SPEC: String = "spec.md"

        const val CB7_COPY: String = "comic_copy.cb7"

        const val CB7_LZMA: String = "comic_lzma2.cb7"
    }
}

internal fun isDjvuDocument(document: Document): Boolean {
    return document is DjvuDocument
}
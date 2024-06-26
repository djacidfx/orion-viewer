package universe.constellation.orion.viewer.test.espresso

import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.SeekBar
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.Controller
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.android.isAtLeastKitkat
import universe.constellation.orion.viewer.lastPageNum0
import universe.constellation.orion.viewer.test.framework.BaseTestWithActivity
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.BookFile
import universe.constellation.orion.viewer.test.framework.LONG_TIMEOUT
import universe.constellation.orion.viewer.test.framework.SHORT_TIMEOUT
import universe.constellation.orion.viewer.test.framework.appContext
import universe.constellation.orion.viewer.test.framework.onActivity

/*Default zoom is "Fit Width"*/
abstract class BaseViewerActivityTest(
    val bookDescription: BookFile,
    startIntent: Intent = bookDescription.toOpenIntent(),
    private val config: Configuration = DefaultConfig
) : BaseTestWithActivity(startIntent) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Test for {0} book")
        fun testData(): Iterable<BookFile> {
            return BookFile.testEntriesWithCustoms()
        }
    }

    private lateinit var controller: Controller

    @Before
    fun grantPermissionsAndProcessErrors() {
        processEmulatorErrors()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !BookDescription.SICP.asFile().canRead()) {
            val grant =
                device.wait(Until.findObject(By.textContains("Grant")), LONG_TIMEOUT) ?: run {
                    //in case of problem with system UI
                    device.wait(Until.findObject(By.textContains("Wait")), SHORT_TIMEOUT)?.click()
                    device.wait(Until.findObject(By.textContains("Grant")), LONG_TIMEOUT)
                        ?: error("Can't find grant action in warning dialog")
                }

            grant.click()

            val allowField = device.wait(Until.findObject(By.textContains("Allow")), LONG_TIMEOUT)
            allowField.click()
            device.wait(Until.findObject(By.checkable(true)), LONG_TIMEOUT)
            Assert.assertTrue(device.findObject(By.checkable(true)).isChecked)
            device.pressBack()
            Assert.assertTrue(BookDescription.SICP.asFile().canRead())
        }

        checkStartInvariant()

        with(config) {
            body()
        }
    }

    private fun checkStartInvariant() {
        Espresso.onIdle()
        activityScenarioRule.scenario.onActivity {
            controller = it.controller!!
            Assert.assertEquals(it.controller!!.document.filePath, bookDescription.asPath())
            Assert.assertFalse(controller.pageLayoutManager.sceneRect.isEmpty)
        }
    }

    @After
    fun checkEndInvariant() {
        onActivity {
            if (::controller.isInitialized) {
                Assert.assertEquals(it.controller!!, controller)
            }
        }
    }

    protected fun applyGoTo() {
        activityScenarioRule.scenario.applyGoTo()
    }

    protected fun openZoom() {
        activityScenarioRule.scenario.openZoom()
    }

    protected fun openGoTo() {
        activityScenarioRule.scenario.openGoTo()
    }


    private fun openMenuAndSelect(id: Int, resId: Int) {
        activityScenarioRule.scenario.openMenuAndSelect(id, resId)
    }

    protected val currentPage0: Int
        get() = onActivity {
            it.controller!!.currentPage
        }

    val lastPageNumber0: Int
        get() = onActivity {
            it.controller!!.lastPageNum0
        }
}

internal fun setSeekBarProgress(transform: (Int) -> Int): ViewAction {
    return object : ViewAction {
        override fun perform(uiController: UiController?, view: View) {
            val seekBar = view as SeekBar
            val transform1 = transform(seekBar.progress)
            seekBar.progress = transform1
        }

        override fun getDescription(): String {
            return "Set a progress on a SeekBar"
        }

        override fun getConstraints(): Matcher<View> {
            return ViewMatchers.isAssignableFrom(SeekBar::class.java)
        }
    }
}
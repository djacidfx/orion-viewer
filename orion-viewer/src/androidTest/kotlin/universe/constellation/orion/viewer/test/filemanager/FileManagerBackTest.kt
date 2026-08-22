package universe.constellation.orion.viewer.test.filemanager

import android.os.SystemClock
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import universe.constellation.orion.viewer.OrionFileSelectorActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.test.framework.BaseTest

/**
 * Back is served by an [androidx.activity.OnBackPressedCallback] enabled only while the drawer is
 * open, so both states are checked: a callback that never gets disabled traps the user in the file
 * manager, a callback that never gets enabled makes back leave it with the drawer still open.
 *
 * The drawer lives in the shared file manager base class, so the lightweight selector activity
 * covers the very same code as the main file manager.
 */
class FileManagerBackTest : BaseTest() {

    @Test
    fun backClosesOpenedDrawer() {
        ActivityScenario.launch(OrionFileSelectorActivity::class.java).use { scenario ->
            scenario.onActivity {
                it.drawer.openDrawer(GravityCompat.START, false)
                assertTrue("Drawer wasn't opened", it.drawer.isDrawerOpen(GravityCompat.START))

                it.onBackPressedDispatcher.onBackPressed()
            }

            //closing is animated, so the drawer stays reported as opened for a while after back
            await("Drawer is still opened after back") {
                var closed = false
                scenario.onActivity { closed = !it.drawer.isDrawerOpen(GravityCompat.START) }
                closed
            }
            assertEquals(
                "File manager shouldn't be left with the opened drawer",
                Lifecycle.State.RESUMED,
                scenario.state
            )
        }
    }

    @Test
    fun backLeavesFileManagerWithClosedDrawer() {
        ActivityScenario.launch(OrionFileSelectorActivity::class.java).use { scenario ->
            scenario.onActivity {
                assertFalse(
                    "Drawer is expected to be closed on start",
                    it.drawer.isDrawerOpen(GravityCompat.START)
                )

                it.onBackPressedDispatcher.onBackPressed()
            }

            //predictive back leaves through finishAfterTransition, so finishing isn't immediate
            await("File manager wasn't left on back") {
                scenario.state == Lifecycle.State.DESTROYED
            }
        }
    }

    private fun await(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + AWAIT_TIMEOUT
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            SystemClock.sleep(POLL_INTERVAL)
        }
        throw AssertionError("$message in $AWAIT_TIMEOUT ms")
    }

    private val OrionFileSelectorActivity.drawer: DrawerLayout
        get() = findViewById(R.id.drawer_layout)

    companion object {
        private const val AWAIT_TIMEOUT = 5000L

        private const val POLL_INTERVAL = 50L
    }
}

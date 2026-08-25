package universe.constellation.orion.viewer.test.espresso.contenturi

import android.os.Build
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.filters.SdkSuppress
import org.junit.Assert
import org.junit.Test
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.test.framework.BaseTestWithActivity
import universe.constellation.orion.viewer.test.framework.createContentIntentWithGeneratedFile
import universe.constellation.orion.viewer.test.framework.onActivity

/**
 * The provider throws for this name on every open, like a file the sharing app has dropped.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
class InvalidContentProviderTest : BaseTestWithActivity(createContentIntentWithGeneratedFile("secondTime.error.pdf")) {

    @Test
    fun unreadableSourceIsReportedWithoutCopyOptions() {
        onView(withText(R.string.fileopen_source_unavailable)).check(matches(isDisplayed()))
        onView(withSubstring("FileNotFoundException")).check(matches(isDisplayed()))
        //copying can't succeed either, so it's not offered, and there is nothing to report
        onView(withText(R.string.fileopen_open_in_temporary_file)).check(doesNotExist())
        onView(withText(R.string.fileopen_save_to_file)).check(doesNotExist())
        onView(withText(R.string.fileopen_report_error_by_github_and_return)).check(doesNotExist())

        onView(withText(R.string.string_cancel)).perform(click())
        onView(withId(R.id.problem_view)).check(matches(isCompletelyDisplayed()))
        onView(withSubstring("FileNotFoundException")).check(matches(isDisplayed()))
        onActivity {
            Assert.assertNull(it.controller)
        }
    }

}

/**
 * The provider serves this name once and throws afterwards: the file was readable when the
 * fallback dialog was built, but is gone by the time the copy starts.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
class InvalidContentProvider2Test : BaseTestWithActivity(createContentIntentWithGeneratedFile("secondTime.error2.pdf")) {

    @Test
    fun sourceVanishedBeforeCopyIsReportedWithoutCrashDialog() {
        onTextNotButtonView(R.string.fileopen_open_in_temporary_file).perform(click())

        onView(withSubstring("FileNotFoundException")).check(matches(isDisplayed()))
        //a plain message: no report to send
        onView(withId(R.id.crash_send_github)).check(doesNotExist())
        onView(withText(R.string.string_close)).perform(click())
        onActivity {
            Assert.assertNull(it.controller)
        }
    }
}

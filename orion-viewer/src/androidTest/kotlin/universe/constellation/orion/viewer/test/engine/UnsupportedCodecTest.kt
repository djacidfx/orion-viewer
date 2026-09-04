package universe.constellation.orion.viewer.test.engine

import android.graphics.Point
import org.junit.Assert.assertNotEquals
import org.junit.Test
import universe.constellation.orion.viewer.FileUtil.openFile
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.test.framework.BookFile

/* Codecs we deliberately don't ship (e.g. bzip2 inside 7z) must fail
 * predictably instead of rendering garbage. Books live in the unsupported/
 * subfolder so that folder-scanning tests don't pick them up. */
class UnsupportedCodecTest : BaseTest() {

    @Test
    fun bzip2InsideCb7FailsPredictably() {
        val book = BookFile("unsupported/comic_bzip2.cb7")
        val document = try {
            book.openBook()
        } catch (e: Exception) {
            /* Expected without liblzma: even the archive header is unreadable. */
            return
        }
        try {
            /* With liblzma the header opens, but page data is bzip2-compressed:
             * decoding the page must fail rather than produce a real page. */
            val page = document.getOrCreatePageAdapter(0)
            try {
                val size = page.getPageSize().run { Point(width, height) }
                assertNotEquals(
                    "bzip2-compressed page unexpectedly decoded",
                    Point(300, 450),
                    size
                )
            } finally {
                page.destroy()
            }
        } catch (e: Exception) {
            /* Expected: page decoding fails. */
        } finally {
            document.destroy()
        }
    }
}

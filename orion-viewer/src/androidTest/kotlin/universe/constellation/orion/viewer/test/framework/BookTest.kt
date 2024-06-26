package universe.constellation.orion.viewer.test.framework

import android.content.Intent
import android.graphics.Point
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.FileUtil.openFile
import universe.constellation.orion.viewer.test.framework.BaseTest.Companion.ALICE
import universe.constellation.orion.viewer.test.framework.BaseTest.Companion.DJVU_SPEC
import universe.constellation.orion.viewer.test.framework.BaseTest.Companion.SICP
import java.io.File

@RunWith(Parameterized::class)
abstract class BookTest(protected val bookDescription: BookDescription) : BaseTest() {

    private val documentDelegate = lazy { bookDescription.openBook() }

    protected val document by documentDelegate

    @After
    fun close() {
        if (documentDelegate.isInitialized()) {
            document.destroy()
        }
    }
}

open class BookFile(val simpleFileName: String) {
    fun toOpenIntent(body: Intent.() -> Unit = {}): Intent {
        return createTestViewerIntent {
            data = Uri.fromFile(asFile())
            body()
        }
    }

    fun asFile() = File(BaseTest.testDataFolder, simpleFileName)

    fun asAssetsFileStream() =
        InstrumentationRegistry.getInstrumentation().context.resources.assets.open(BaseTest.testDataFolder.name + "/" + simpleFileName)

    fun asPath() = asFile().absolutePath

    fun openBook() = openFile(asFile())

    override fun toString(): String {
        return "BookFile(path='$simpleFileName')"
    }

    companion object {

        private val EXTENDED_HARD_CODED_TEST = System.getenv("test.books.extended")?.toBoolean() ?: false

        private val DEFAULT_BOOKS = listOf(
            ALICE,
            DJVU_SPEC,
            SICP
        )

        fun testEntriesWithCustoms(): List<BookFile> {
            val files = BaseTest.testDataFolder.listFiles()
            if (files.isNullOrEmpty() || files.count { it.isFile } < DEFAULT_BOOKS.size) return hardCodedEntries()
            return files.mapNotNull {
                if (it.isDirectory) {
                    null
                } else {
                    BookFile(it.name)
                }
            }
        }

        private fun hardCodedEntries(): List<BookFile> {
            return (if (EXTENDED_HARD_CODED_TEST)
                listOf(
                ALICE,
                "an1.pdf",
                "cr1.pdf",
                "cr2.pdf",
                "decode.pdf",
                DJVU_SPEC,
                "h1.djvu",
                "oz1.pdf",
                "quest.pdf",
                SICP
            ) else {
                DEFAULT_BOOKS
            }).map {
                BookFile(it)
            }
        }
    }
}

sealed class BookDescription(
        path: String,
        val pageCount: Int,
        val title: String?,
        val topLevelOutlineItems: Int,
        val allOutlineItems: Int = topLevelOutlineItems,
        val pageSize: Point = Point(0, 0)
): BookFile(path) {

    data object SICP: BookDescription(BaseTest.SICP, 762, "", 15, 139, Point(662, 885))
    data object ALICE: BookDescription(BaseTest.ALICE, 77, null, 0,  pageSize = Point(2481, 3508))
    data object DJVU_SPEC: BookDescription(BaseTest.DJVU_SPEC, 71, null, 1, 100, Point(2539, 3295))

    companion object {
        fun testData(): List<BookDescription> {
            return if (MANUAL_DEBUG) {
                listOf(SICP)
            } else {
                listOf(SICP, ALICE, DJVU_SPEC)
            }
        }
    }
}
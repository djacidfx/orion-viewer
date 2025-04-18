/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
 *
 * Copyright (C) 2011-2017  Michael Bogdanov & Co
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package universe.constellation.orion.viewer.pdf

import android.graphics.RectF
import androidx.core.graphics.toRect
import com.artifex.mupdf.fitz.Device
import com.artifex.mupdf.fitz.DisplayList
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.Outline
import com.artifex.mupdf.fitz.Page
import com.artifex.mupdf.fitz.StructuredText
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import com.artifex.mupdf.viewer.MuPDFCore
import com.artifex.mupdfdemo.TextWord
import universe.constellation.orion.viewer.Bitmap
import universe.constellation.orion.viewer.PageSize
import universe.constellation.orion.viewer.document.AbstractDocument
import universe.constellation.orion.viewer.document.AbstractPage
import universe.constellation.orion.viewer.document.OutlineItem
import universe.constellation.orion.viewer.document.PageText
import universe.constellation.orion.viewer.document.PageTextBuilder
import universe.constellation.orion.viewer.errorInDebug
import universe.constellation.orion.viewer.errorInDebugOr
import universe.constellation.orion.viewer.timing

class PdfDocument @Throws(Exception::class) constructor(filePath: String) : AbstractDocument(filePath) {

    inner class PdfPage(pageNum: Int) : AbstractPage(pageNum) {
        @Volatile
        private var page: Page? = null
        private var displayList: DisplayList? = null

        @Volatile
        private var pageTextBuilder: PageTextBuilder? = null

        private fun readPageDataIfNeeded() {
            if (destroyed) return errorInDebug("Page $pageNum already destroyed")
            if (page == null) {
                synchronized(core) {
                    if (page == null) {
                        try {
                            timing("Page extraction: $pageNum") {
                                page = core.doc.loadPage(pageNum)
                            }
                        } catch (e: IllegalArgumentException) {
                            if (e.message == "page number out of range") {
                                throw IllegalArgumentException("page number out of range: $pageNum of ${this@PdfDocument.pageCount}")
                            }
                            throw e;
                        }
                    }
                }
            }
        }

        override fun readPageSize(): PageSize? {
            readPageDataIfNeeded()
            val bbox = page?.bounds ?: return null
                ?: errorInDebugOr("Problem extracting page dimension") { return null }
            val pageWidth = bbox.x1 - bbox.x0
            val pageHeight = bbox.y1 - bbox.y0
            return PageSize(pageWidth.toInt(), pageHeight.toInt())
        }

        override fun renderPage(
            bitmap: Bitmap,
            zoom: Double,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            leftOffset: Int,
            topOffset: Int
        ) {
            if (destroyed) return
            readPageDataForRendering()
            if (displayList == null) return
            val dev: Device =
                AndroidDrawDevice(bitmap, leftOffset, topOffset, left, top, right, bottom)
            try {
                val zoom1 = zoom.toFloat()
                displayList!!.run(dev, Matrix(zoom1, zoom1), null)
                updateContrast(
                    bitmap,
                    top,
                    left,
                    bottom,
                    right,
                    bitmap.width
                )
                dev.close()
            } finally {
                dev.destroy()
            }
        }

        private fun getOrCreateDisplayList() {
            if (displayList != null) return
            synchronized(core) {
                displayList = page?.toDisplayList()
            }
        }

        override fun readPageDataForRendering() {
            readPageDataIfNeeded()
            getOrCreateDisplayList()
        }

        override fun destroyInternal() {
            if (displayList != null) {
                displayList?.destroy()
                displayList = null
            }
            if (page != null) {
                page?.destroy()
                page = null
            }
        }

        override fun searchText(text: String): Array<RectF>? {
            readPageDataIfNeeded()

            return (page ?: errorInDebugOr("No page") {return null}).let {
                core.searchPage(page, text)?.map { it.toRect().run { RectF(x0, y0, x1, y1) } }?.toTypedArray()
            }
        }

        override fun getPageText(): PageText? {
            if (destroyed) return null
            readPageDataIfNeeded()
            if (page == null) return null

            if (pageTextBuilder == null) {
                pageTextBuilder = getTextInfo(page!!) ?: PageTextBuilder.NULL
            }

            val builder = pageTextBuilder
            if (builder == PageTextBuilder.NULL) {
                return null
            }
            return builder
        }

        override fun destroy() {
            destroyPage(this)
        }
    }


    private val core = MuPDFCore(filePath)

    override val pageCount: Int
        get() = core.countPages()

    override fun createPage(pageNum: Int): AbstractPage {
        return PdfPage(pageNum)
    }

    override fun destroy() = core.onDestroy()

    override val title: String? by lazy {
        core.title
    }

    external override fun setContrast(contrast: Int)
    external fun updateContrast(bitmap: Bitmap, startRow: Int, startCol: Int, endRow: Int, endCol: Int, width: Int)

    external override fun setThreshold(threshold: Int)

    private fun getTextInfo(page: Page): PageTextBuilder? {
        val text: StructuredText = synchronized(core) { page.toStructuredText() }
        try {
           return buildTextInfo(text)
        } finally {
            text.destroy()
        }
    }

    private fun buildTextInfo(text: StructuredText): PageTextBuilder {
        val pageTextBuilder = PageTextBuilder()//TODO: cache
        for (block in text.blocks) {
            if (block != null) {
                for (ln in block.lines) {
                    pageTextBuilder.newLine()
                    if (ln != null) {
                        var word = TextWord()
                        pageTextBuilder.addSpace()
                        for (textChar in ln.chars) {
                            if (textChar.c != ' '.code) {
                                word.add(textChar)
                            } else if (word.isNotEmpty()) {
                                if (word.isNotEmpty()) {
                                    pageTextBuilder.addWord(word.toString(), word.rect.toRect())
                                    pageTextBuilder.addSpace()
                                }
                                word = TextWord()
                            }
                        }
                        if (word.isNotEmpty()) {
                            pageTextBuilder.addWord(word.toString(), word.rect.toRect())
                        }
                    }
                }
            }
        }

        return pageTextBuilder
    }

    override val outline: Array<OutlineItem>?
        get() {
            fun collectItems(list: MutableList<OutlineItem>, items: Array<Outline>, level: Int) {
                items.forEach {
                    list.add(OutlineItem(level, it.title ?: "<Empty>", core.pageNumberFromOutline(it)))
                    if (it.down != null) {
                        collectItems(list, it.down, level + 1)
                    }
                }
            }
            return core.outline?.takeIf { it.isNotEmpty() }?.let { items ->
                ArrayList<OutlineItem>(items.size).apply { collectItems(this, items, 0) }.toTypedArray()
            }

        }

    override fun needPassword() = core.needsPassword()

    override fun authenticate(password: String) = core.authenticatePassword(password)

}

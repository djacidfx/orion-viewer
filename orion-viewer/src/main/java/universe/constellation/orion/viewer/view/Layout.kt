package universe.constellation.orion.viewer.view

import android.graphics.PointF
import android.graphics.Rect
import universe.constellation.orion.viewer.Controller
import universe.constellation.orion.viewer.PageView
import universe.constellation.orion.viewer.log
import universe.constellation.orion.viewer.selection.PageAndSelection

class PageLayoutManager(val controller: Controller, val scene: OrionDrawScene): ViewDimensionAware {

    init {
        scene.setDimensionAware(this)
    }

    val visiblePages: MutableList<PageView> = arrayListOf()

    var isSinglePageMode = false

    val sceneRect = Rect(0, 0, scene.width, scene.height)
    val tmpRect = Rect(0, 0, scene.width, scene.height)
    var sceneWidth: Int
        get() = scene.width
        set(value) {
            sceneRect.right = value
        }
    var sceneHeight: Int
        get() = scene.height
        set(value)  {
            sceneRect.bottom = value
        }

    override fun onDimensionChanged(newWidth: Int, newHeight: Int) {
        if (sceneWidth != newWidth && newHeight != sceneHeight) {
            sceneWidth = newWidth
            sceneHeight = newHeight
            updateRenderingParameters()
        }
    }

    fun updateRenderingParameters() {
        //zoom
        isSinglePageMode = false
        uploadNewPages()
    }

    fun destroy() {
        visiblePages.forEach {
            it.toInvisibleState()
        }
        visiblePages.clear()
    }

    fun doScroll(xPos: Float, yPos: Float, distanceX: Float, distanceY: Float) {
        isSinglePageMode = false
        val iterator = visiblePages.iterator()
        iterator.forEach {
            it.layoutData.position.y += distanceY
            if (it.layoutData.contains(xPos, yPos)) {
                val screenPart = it.layoutData.visibleOnScreenPart(sceneRect)
                if (distanceX < 0 && screenPart.right < sceneRect.right) {
                    //nothing
                } else if (distanceX > 0 && screenPart.left > sceneRect.left) {
                    //nothing
                } else {
                    if (!it.layoutData.insideScreenX(sceneRect)) {
                        it.layoutData.position.x += distanceX
                    }
                }
            }
            if (!updateStateAndRender(it)) {
                iterator.remove()
                println("Remove ${it.pageNum}")
            }
        }
        dump()
        uploadNewPages()
        scene.postInvalidate()
    }

    fun uploadNewPages() {        //zoom
        if (isSinglePageMode) return
        println("Before uploadNewPages")
        dump()
        if (visiblePages.isEmpty()) {
            val nextPageNum = 0
            val view = controller.createCachePageView(nextPageNum)
            addPageInPosition(view, 0f)
        } else {
            val lastView = visiblePages.last()
            val pageEnd = lastView.layoutData.position.y + lastView.layoutData.wholePageRect.height() + 1
            if (pageEnd < sceneHeight) {
                val pageNum = lastView.pageNum + 1
                if (pageNum < controller.pageCount) {
                    addPageInPosition(pageNum, pageEnd)
                }
            }

            val firstView = visiblePages.first()
            val pageStart = firstView.layoutData.position.y
            println("Check new $pageStart ${firstView.pageNum} ${visiblePages.size}")
            if (pageStart > 5 && firstView.pageNum > 0) {
                val newView = controller.createCachePageView(firstView.pageNum - 1)
                addPageInPosition(newView, firstView.layoutData.position.y - newView.layoutData.wholePageRect.height() - 2, false)
            }
        }
        println("After uploadNewPages")
        dump()
    }

    private fun addPageInPosition(pageNum: Int, pageYPos: Float) {
        val view = controller.createCachePageView(pageNum)
        addPageInPosition(view, pageYPos)
    }

    private fun addPageInPosition(view: PageView, pageYPos: Float, last: Boolean = true) {
        view.layoutData.position.y = pageYPos
        if (last) {
            visiblePages.add(view)
        } else {
            visiblePages.add(0, view)
        }
        updateStateAndRender(view)
    }

    fun findPageAndPageRect(screenRect: Rect): List<PageAndSelection> {
        return visiblePages.mapNotNull {
            val visibleOnScreenPart = it.layoutData.visibleOnScreenPart(screenRect)
            if (visibleOnScreenPart.intersect(screenRect)) {
                val pageSelection = Rect(visibleOnScreenPart)
                //TODO zoom and crop
                pageSelection.minusOffset(it.layoutData.position)
                val layoutInfo = it.layoutInfo
                val zoom  = layoutInfo.docZoom
                pageSelection.offset(layoutInfo.x.marginLess, layoutInfo.y.marginLess)
                pageSelection.set((pageSelection.left/zoom).toInt(),
                    (pageSelection.top/zoom).toInt(),
                    (pageSelection.right/zoom).toInt(), (pageSelection.bottom/zoom).toInt()
                )
                PageAndSelection(it.pageNum, pageSelection)
            } else {
                null
            }
        }
    }

    fun showPageWithOffset(pageNum: Int, x: Int, y: Int) {
        isSinglePageMode = true
        val iterator = visiblePages.iterator()
        for (page in iterator) {
            if (page.pageNum != pageNum) {
                page.toInvisibleState()
                iterator.remove()
            }
        }
        val page = if (visiblePages.isNotEmpty()) {
            visiblePages.first()
        } else {
            val page = controller.createCachePageView(pageNum)
            visiblePages.add(page)
            page
        }

        //TODO keep proper position
        page.layoutData.position.set(x.toFloat(), y.toFloat())
        page.renderVisible()
    }

    fun dump() {
        visiblePages.forEach{
            println("Dump ${it.pageNum}: ${it.layoutData.position} ${it.layoutData.wholePageRect}")
        }

        visiblePages.zipWithNext().forEach {
            val visibleOnScreenPart = it.first.layoutData.visibleOnScreenPart(sceneRect)
            val visible2 = it.second.layoutData.visibleOnScreenPart(sceneRect)
            if (visibleOnScreenPart.intersect(visible2)) {
                log("intersection: $visibleOnScreenPart")
                log("intersection: ${it.first.layoutData.visibleOnScreenPart(sceneRect)} $visible2")
                visibleOnScreenPart.intersect(visible2)
                error("intersected rects ${it.first.pageNum} and ${it.second.pageNum}: $visibleOnScreenPart")
            }
        }
    }

    private fun updateStateAndRender(view: PageView): Boolean {
        if (isVisible(view)) {
            view.renderVisible()
            return true
        } else {
            println("toInvisible ${view.pageNum}")
            view.toInvisibleState()
            return false
        }
    }

    fun isVisible(view: PageView): Boolean {
        return isVisible(view.wholePageRect, view.layoutData.position)
    }

    fun isVisible(pageRect: Rect, pos: PointF): Boolean {
        tmpRect.set(pageRect)
        tmpRect.offset(pos.x.toInt(), pos.y.toInt())
        return tmpRect.intersect(sceneRect)
    }

    fun onSizeCalculated(updatedView: PageView, oldArea: Rect) {
        log("onSizeCalculated ${updatedView.pageNum}: ${updatedView.layoutData} $oldArea")
        var found = false
        var shiftY = 0
        if (updatedView.layoutData.position.y < 0) {
            if (isVisible(oldArea, updatedView.layoutData.position)) {
                shiftY = oldArea.height() - updatedView.wholePageRect.height()
                updatedView.layoutData.position.y += shiftY
                updatedView.layoutData.wholePageRect.set(updatedView.wholePageRect)
                dump()
            } else {
                visiblePages.remove(updatedView)
            }
        }
        else {
            for (page in visiblePages) {
                if (found) {
                    page.layoutData.position.y += shiftY
                } else if (page == updatedView) {
                    found = true
                    val viewDimension = page.layoutData.wholePageRect
                    shiftY = page.wholePageRect.height() - viewDimension.height()
                    viewDimension.set(page.wholePageRect)
                }
            }
        }
        updateStateAndRender(updatedView)
        scene.invalidate()
    }

    fun onViewSizeChanged() {
        //TODO find old anchor and use it for relayouting
    }

    fun defaultSize(): Rect {
        val rect = Rect(0, 0, scene.width, scene.height)
        if (rect.width() == 0) {
            rect.right = 400
        }

        if (rect.height() == 0) {
            rect.bottom = 400
        }
        return rect
    }
}

fun Rect.offset(p: PointF) {
    this.offset(p.x.toInt(), p.y.toInt())
}

fun Rect.minusOffset(p: PointF) {
    this.offset(-p.x.toInt(), -p.y.toInt())
}
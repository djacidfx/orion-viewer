package universe.constellation.orion.viewer.view

import android.graphics.Rect
import android.graphics.Region
import universe.constellation.orion.viewer.PageState
import universe.constellation.orion.viewer.PageView

suspend fun PageView.precache() {
    if (this.state != PageState.SIZE_AND_BITMAP_CREATED) return

    val visibleRect = this.visibleRect() ?: return
    val top = visibleRect.top
    val bottom = visibleRect.bottom
    val left = visibleRect.left
    val right = visibleRect.right
    val sceneInfo = pageLayoutManager.sceneRect

    val deltaX = sceneInfo.width().upperHalf / 2
    val deltaY = sceneInfo.height().upperHalf / 2
    val t = Rect(left - deltaX, top - deltaY, right + deltaX, top)
    renderInvisible(t)
    val b = Rect(left - deltaX, bottom, right + deltaX, bottom + deltaY)
    renderInvisible(b)
    val l = Rect(left - deltaX, top, left, bottom)
    renderInvisible(l)
    val r = Rect(right, top, right + deltaX, bottom)
    renderInvisible(r)

    this.pageLayoutManager.uploadNextPage(this, true)
    this.pageLayoutManager.uploadPrevPage(this, true)
}

fun Rect.screenForPrecache(pageLayoutManager: PageLayoutManager) {
    val sceneInfo = pageLayoutManager.sceneRect
    set(pageLayoutManager.sceneRect)
    inset(-sceneInfo.width().upperHalf / 2, -sceneInfo.height().upperHalf / 2)
}

internal val Int.upperHalf
    get(): Int {
        return this / 2 + this % 2
    }
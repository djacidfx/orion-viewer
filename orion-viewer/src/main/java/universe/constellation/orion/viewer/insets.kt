package universe.constellation.orion.viewer

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Reserves room for the system bars as padding of the view itself: the background keeps spanning
 * the bars, only the content is inset.
 *
 * Insets are not consumed, so views nesting their own inset aware children still see them.
 */
fun View.padWithSystemBars() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val bars = windowInsets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        view.updatePadding(
            left = bars.left,
            top = bars.top,
            right = bars.right,
            bottom = bars.bottom
        )
        windowInsets
    }
    ViewCompat.requestApplyInsets(this)
}

package universe.constellation.orion.viewer

import universe.constellation.orion.viewer.selection.ClickInfo
import universe.constellation.orion.viewer.selection.ClickType

enum class ContextAction(customName: String? = null) {

    //now just do word selection
    SELECT_TEXT {
        override fun doAction(activity: OrionViewerActivity, clickInfo: ClickInfo) {
            val pos = clickInfo as? ClickInfo ?: return
            val selectionAutomata = activity.selectionAutomata
            selectionAutomata.initSelectionByPosition(pos, false)
        }
    },

    SELECT_WORD_AND_TRANSLATE {

        override fun doAction(activity: OrionViewerActivity, clickInfo: ClickInfo) {
            val pos = clickInfo as? ClickInfo ?: return
            val selectionAutomata = activity.selectionAutomata
            selectionAutomata.initSelectionByPosition(pos, true)
        }
    },

    TAP_ACTION {
        override fun doAction(activity: OrionViewerActivity, clickInfo: ClickInfo) {
            val width = activity.view.sceneWidth
            val height = activity.view.sceneHeight
            if (height == 0 || width == 0) return

            val i = 3 * clickInfo.y / height
            val j = 3 * clickInfo.x / width

            val code = activity.globalOptions.getActionCode(i, j, clickInfo.clickType == ClickType.LONG)
            activity.doAction(code)
        }
    };

    val key = customName ?: name

    open fun doAction(activity: OrionViewerActivity, clickInfo: ClickInfo) {

    }

    companion object {
        fun findAction(action: String): ContextAction? {
            return entries.firstOrNull { it.key == action }
        }
    }
}
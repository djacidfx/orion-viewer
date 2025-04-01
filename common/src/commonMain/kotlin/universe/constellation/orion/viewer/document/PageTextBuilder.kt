package universe.constellation.orion.viewer.document

import android.graphics.Rect

interface PageText {
    val lines: List<List<TextWord>>
}

class PageTextBuilder : PageText {

    companion object {

        val NULL = PageTextBuilder()

        val space = TextWord().apply { add(" ", Rect()) }
    }

    override val lines = mutableListOf<MutableList<TextWord>>()

    fun addWord(value: String, x: Int, y: Int, x2: Int, y2: Int) {
        lastLine().add(TextWord().apply { add(value, Rect(x, y, x2, y2)) })
    }

    fun addWord(value: String, rect: Rect) {
        lastLine().add(TextWord().apply { add(value, rect) })
    }

    private fun lastLine(): MutableList<TextWord> {
        if (lines.isEmpty()) {
            lines.add(mutableListOf())
        }
        return lines.last()
    }

    fun newLine() {
        if (lastLine().isNotEmpty()) {
            lines.add(mutableListOf())
        }
    }

    fun addSpace() {
        lastLine().add(space)
    }
}
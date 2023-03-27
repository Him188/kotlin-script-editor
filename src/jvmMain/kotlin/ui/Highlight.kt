package ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString


interface Highlighter {
    fun highlight(source: String): AnnotatedString
}

class KotlinHighlighter(
    private val keywordStyle: SpanStyle,
) : Highlighter {

    private val keywords = listOf(
        "val", "var", "class", "interface",
        "is", "as", "in",
        "for", "if", "while",
    )

    override fun highlight(source: String): AnnotatedString = buildAnnotatedString {
        if (source.isEmpty() || source.indexOf(' ') == -1) {
            append(source)
            return@buildAnnotatedString
        }
        for (word in source.splitToSequence(' ')) {
            if (word in keywords) {
                append(AnnotatedString(word, keywordStyle))
            } else {
                append(word)
            }
            append(' ')
        }
    }
}

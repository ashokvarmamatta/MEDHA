package com.ashes.dev.works.ai.neural.brain.medha.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Hand-rolled, dependency-free markdown renderer for LLM output.
 *
 * Parses [markdown] line-by-line into blocks and renders them with Compose Material3.
 *
 * Supported block types: fenced code blocks, headings, unordered and ordered
 * lists (with nested indentation), blockquotes, horizontal rules and paragraphs.
 * Supported inline markup: bold, italic, inline code, strikethrough and links.
 *
 * @param onCopyCode optional callback invoked (in addition to copying to the clipboard)
 *                   when a code block's copy button is tapped.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    baseStyle: TextStyle = LocalTextStyle.current,
    onCopyCode: ((String) -> Unit)? = null
) {
    if (markdown.isEmpty()) return

    val blocks = rememberOrParse(markdown)

    if (blocks == null) {
        // Parsing failed unexpectedly — fall back to plain text so we never crash.
        Text(text = markdown, modifier = modifier, color = color, style = baseStyle)
        return
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (block in blocks) {
            when (block) {
                is MdBlock.CodeBlock -> CodeBlockView(block, onCopyCode)
                is MdBlock.Heading -> HeadingView(block, color, baseStyle)
                is MdBlock.ListItem -> ListItemView(block, color, baseStyle)
                is MdBlock.Quote -> QuoteView(block, color, baseStyle)
                MdBlock.Rule -> HorizontalDivider()
                is MdBlock.Paragraph -> ParagraphView(block, color, baseStyle)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Block model
// ---------------------------------------------------------------------------

private sealed interface MdBlock {
    /** Fenced code block. [code] preserves original whitespace/newlines verbatim. */
    data class CodeBlock(val language: String?, val code: String) : MdBlock

    /** ATX heading. [level] is 1..3 (deeper levels clamp to 3). */
    data class Heading(val level: Int, val text: String) : MdBlock

    /**
     * A single list item.
     * @param indent nesting level (2 leading spaces == one level)
     * @param ordered true for ordered lists; [marker] then holds the number label
     * @param marker the bullet/number to show (e.g. "•" or "3.")
     */
    data class ListItem(
        val indent: Int,
        val ordered: Boolean,
        val marker: String,
        val text: String
    ) : MdBlock

    data class Quote(val text: String) : MdBlock

    data object Rule : MdBlock

    data class Paragraph(val text: String) : MdBlock
}

// ---------------------------------------------------------------------------
// Parser
// ---------------------------------------------------------------------------

/** Safe wrapper: returns null on any unexpected failure so callers can fall back. */
@Composable
private fun rememberOrParse(markdown: String): List<MdBlock>? {
    // Not using remember() keyed on markdown to keep semantics dead-simple and robust;
    // parsing is cheap and runs each recomposition. Returns null on failure.
    return try {
        parseMarkdown(markdown)
    } catch (t: Throwable) {
        null
    }
}

private fun parseMarkdown(markdown: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = markdown.replace("\r\n", "\n").replace("\r", "\n").split("\n")

    var i = 0
    val paragraphBuffer = StringBuilder()

    fun flushParagraph() {
        if (paragraphBuffer.isNotEmpty()) {
            val text = paragraphBuffer.toString().trim()
            if (text.isNotEmpty()) {
                blocks.add(MdBlock.Paragraph(text))
            }
            paragraphBuffer.setLength(0)
        }
    }

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        // --- Fenced code block ---------------------------------------------
        val fence = fenceInfo(trimmed)
        if (fence != null) {
            flushParagraph()
            val language = fence.ifBlank { null }
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && fenceInfo(lines[i].trim()) == null) {
                codeLines.add(lines[i])
                i++
            }
            // Skip the closing fence if present.
            if (i < lines.size) i++
            blocks.add(MdBlock.CodeBlock(language, codeLines.joinToString("\n")))
            continue
        }

        // --- Blank line -> paragraph break ---------------------------------
        if (trimmed.isEmpty()) {
            flushParagraph()
            i++
            continue
        }

        // --- Horizontal rule -----------------------------------------------
        if (isHorizontalRule(trimmed)) {
            flushParagraph()
            blocks.add(MdBlock.Rule)
            i++
            continue
        }

        // --- Heading -------------------------------------------------------
        val heading = headingInfo(trimmed)
        if (heading != null) {
            flushParagraph()
            blocks.add(MdBlock.Heading(heading.first, heading.second))
            i++
            continue
        }

        // --- Blockquote ----------------------------------------------------
        if (trimmed.startsWith(">")) {
            flushParagraph()
            blocks.add(MdBlock.Quote(trimmed.removePrefix(">").trimStart()))
            i++
            continue
        }

        // --- List item -----------------------------------------------------
        val listItem = listItemInfo(line)
        if (listItem != null) {
            flushParagraph()
            blocks.add(listItem)
            i++
            continue
        }

        // --- Normal text -> accumulate into a paragraph --------------------
        if (paragraphBuffer.isNotEmpty()) paragraphBuffer.append(' ')
        paragraphBuffer.append(trimmed)
        i++
    }

    flushParagraph()
    return blocks
}

/** Returns the language tag (possibly "") if [trimmed] is a fence line, else null. */
private fun fenceInfo(trimmed: String): String? {
    return if (trimmed.startsWith("```")) {
        trimmed.removePrefix("```").trim()
    } else {
        null
    }
}

private fun isHorizontalRule(trimmed: String): Boolean {
    if (trimmed.length < 3) return false
    val isDashes = trimmed.all { it == '-' }
    val isStars = trimmed.all { it == '*' }
    val isUnderscores = trimmed.all { it == '_' }
    return isDashes || isStars || isUnderscores
}

/** Returns (level, text) for an ATX heading, else null. Level clamped to 1..3. */
private fun headingInfo(trimmed: String): Pair<Int, String>? {
    if (!trimmed.startsWith("#")) return null
    var hashes = 0
    while (hashes < trimmed.length && trimmed[hashes] == '#') hashes++
    if (hashes == 0) return null
    // Must be followed by a space to be a heading.
    if (hashes >= trimmed.length || trimmed[hashes] != ' ') return null
    val level = hashes.coerceIn(1, 3)
    val text = trimmed.substring(hashes).trim()
    return level to text
}

/** Parses a list item from a raw (un-trimmed) [line], else null. */
private fun listItemInfo(line: String): MdBlock.ListItem? {
    val leadingSpaces = line.takeWhile { it == ' ' }.length
    val indent = leadingSpaces / 2
    val content = line.trimStart()

    // Unordered: "-", "*", "+" followed by a space.
    if (content.length >= 2 &&
        (content[0] == '-' || content[0] == '*' || content[0] == '+') &&
        content[1] == ' '
    ) {
        val text = content.substring(2).trim()
        return MdBlock.ListItem(indent = indent, ordered = false, marker = "•", text = text)
    }

    // Ordered: digits followed by "." or ")" then a space.
    val dotIndex = content.indexOfFirst { it == '.' || it == ')' }
    if (dotIndex in 1..9) {
        val numberPart = content.substring(0, dotIndex)
        if (numberPart.all { it.isDigit() } &&
            dotIndex + 1 < content.length &&
            content[dotIndex + 1] == ' '
        ) {
            val text = content.substring(dotIndex + 2).trim()
            return MdBlock.ListItem(
                indent = indent,
                ordered = true,
                marker = "$numberPart.",
                text = text
            )
        }
    }

    return null
}

// ---------------------------------------------------------------------------
// Inline markdown -> AnnotatedString
// ---------------------------------------------------------------------------

private fun buildInline(
    text: String,
    linkColor: Color,
    inlineCodeBg: Color
): AnnotatedString = buildAnnotatedString {
    var i = 0
    val n = text.length

    fun appendStyled(content: String, style: SpanStyle) {
        withStyle(style) {
            // Recurse so combinations like **_bold italic_** work.
            append(buildInline(content, linkColor, inlineCodeBg))
        }
    }

    while (i < n) {
        val c = text[i]

        // Inline code `...` (no nested markdown inside).
        if (c == '`') {
            val close = text.indexOf('`', i + 1)
            if (close > i) {
                val code = text.substring(i + 1, close)
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = inlineCodeBg
                    )
                ) {
                    append(code)
                }
                i = close + 1
                continue
            }
        }

        // Link [label](url)
        if (c == '[') {
            val closeBracket = text.indexOf(']', i + 1)
            if (closeBracket > i &&
                closeBracket + 1 < n &&
                text[closeBracket + 1] == '('
            ) {
                val closeParen = text.indexOf(')', closeBracket + 2)
                if (closeParen > closeBracket) {
                    val label = text.substring(i + 1, closeBracket)
                    withStyle(
                        SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(buildInline(label, linkColor, inlineCodeBg))
                    }
                    i = closeParen + 1
                    continue
                }
            }
        }

        // Bold **...** or __...__
        val boldMarker = matchDelimiter(text, i, "**") ?: matchDelimiter(text, i, "__")
        if (boldMarker != null) {
            appendStyled(boldMarker.content, SpanStyle(fontWeight = FontWeight.Bold))
            i = boldMarker.endExclusive
            continue
        }

        // Strikethrough ~~...~~
        val strike = matchDelimiter(text, i, "~~")
        if (strike != null) {
            appendStyled(strike.content, SpanStyle(textDecoration = TextDecoration.LineThrough))
            i = strike.endExclusive
            continue
        }

        // Italic *...* or _..._
        val italicMarker = matchDelimiter(text, i, "*") ?: matchDelimiter(text, i, "_")
        if (italicMarker != null && italicMarker.content.isNotEmpty()) {
            appendStyled(italicMarker.content, SpanStyle(fontStyle = FontStyle.Italic))
            i = italicMarker.endExclusive
            continue
        }

        // Plain character.
        append(c)
        i++
    }
}

private data class DelimiterMatch(val content: String, val endExclusive: Int)

/**
 * If [text] at [start] opens with [delim] and a matching closing [delim] exists,
 * returns the inner content and the index just past the closing delimiter.
 * Returns null otherwise (so unmatched markers fall through as literal text).
 */
private fun matchDelimiter(text: String, start: Int, delim: String): DelimiterMatch? {
    if (!text.startsWith(delim, start)) return null
    val contentStart = start + delim.length
    if (contentStart >= text.length) return null
    val close = text.indexOf(delim, contentStart)
    if (close < contentStart) return null
    // Avoid treating an immediate empty pair as a match (e.g. "**" alone).
    if (close == contentStart) return null
    val content = text.substring(contentStart, close)
    return DelimiterMatch(content, close + delim.length)
}

// ---------------------------------------------------------------------------
// Block renderers
// ---------------------------------------------------------------------------

@Composable
private fun ParagraphView(block: MdBlock.Paragraph, color: Color, baseStyle: TextStyle) {
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBg = MaterialTheme.colorScheme.surfaceVariant
    Text(
        text = buildInline(block.text, linkColor, codeBg),
        color = color,
        style = baseStyle,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun HeadingView(block: MdBlock.Heading, color: Color, baseStyle: TextStyle) {
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBg = MaterialTheme.colorScheme.surfaceVariant
    val sized = when (block.level) {
        1 -> baseStyle.merge(MaterialTheme.typography.headlineSmall)
        2 -> baseStyle.merge(MaterialTheme.typography.titleLarge)
        else -> baseStyle.merge(MaterialTheme.typography.titleMedium)
    }.copy(fontWeight = FontWeight.Bold)

    Text(
        text = buildInline(block.text, linkColor, codeBg),
        color = color,
        style = sized,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ListItemView(block: MdBlock.ListItem, color: Color, baseStyle: TextStyle) {
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBg = MaterialTheme.colorScheme.surfaceVariant
    val indentDp = (block.indent.coerceAtLeast(0) * 16).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indentDp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = block.marker,
            color = color,
            style = baseStyle,
            modifier = Modifier.width(if (block.ordered) 28.dp else 16.dp)
        )
        Text(
            text = buildInline(block.text, linkColor, codeBg),
            color = color,
            style = baseStyle,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun QuoteView(block: MdBlock.Quote, color: Color, baseStyle: TextStyle) {
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBg = MaterialTheme.colorScheme.surfaceVariant
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .background(MaterialTheme.colorScheme.outline)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = buildInline(block.text, linkColor, codeBg),
            color = if (color == Color.Unspecified) mutedColor else color,
            style = baseStyle,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CodeBlockView(block: MdBlock.CodeBlock, onCopyCode: ((String) -> Unit)?) {
    val clipboard = LocalClipboardManager.current

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header row: language label + copy button.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = block.language ?: "code",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(block.code))
                        onCopyCode?.invoke(block.code)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Code body with horizontal scroll; whitespace preserved verbatim.
            Text(
                text = block.code,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 12.dp)
            )
        }
    }
}

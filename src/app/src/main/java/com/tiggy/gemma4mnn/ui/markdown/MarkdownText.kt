package com.tiggy.gemma4mnn.ui.markdown

import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablesPlugin

/**
 * Markdown rendering composable using Markwon.
 *
 * Wraps a TextView with Markwon for proper Markdown rendering including
 * code blocks, tables, and formatted text.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                val markwon = Markwon.builder(context)
                    .usePlugin(TablesPlugin.create())
                    .build()
                markwon.setMarkdown(this, markdown)
            }
        },
        update = { textView ->
            // Update is called when the markdown content changes
            val markwon = Markwon.builder(textView.context)
                .usePlugin(TablesPlugin.create())
                .build()
            markwon.setMarkdown(textView, markdown)
        },
    )
}

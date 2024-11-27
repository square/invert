package com.squareup.invert.collectors.internal

internal fun String.wrapCodeForMarkdown(language: String = "") = buildString {
  appendLine("```$language")
  appendLine(this@wrapCodeForMarkdown)
  appendLine("```")
}
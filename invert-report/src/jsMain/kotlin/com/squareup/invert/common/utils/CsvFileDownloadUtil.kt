package com.squareup.invert.common.utils

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag

object CsvFileDownloadUtil {
  /**
   * Builds a CSV string. If [headers] is non-empty, it is written as the first line.
   */
  fun createCsvString(rows: List<List<String>>, headers: List<String> = emptyList()): String = buildString {
    if (headers.isNotEmpty()) {
      appendCsvLine(headers)
    }
    rows.forEach { columns ->
      appendCsvLine(columns)
    }
  }

  private fun StringBuilder.appendCsvLine(columns: List<String>) {
    appendLine(columns.joinToString(separator = ",") {
      "\"${it.replace("\"", "\"\"")}\""
    })
  }

  fun downloadFile(content: String, contentType: String, fileName: String) {
    val blob = Blob(arrayOf(content), BlobPropertyBag(contentType))
    window.document.createElement("a").apply {
      setAttribute("href", URL.Companion.createObjectURL(blob))
      setAttribute("download", fileName)
      document.rootElement?.appendChild(this)
      this.dispatchEvent(MouseEvent("click"))
      document.rootElement?.removeChild(this)
    }
  }

  fun downloadCsvFile(content: String, fileName: String = "invert.csv") {
    downloadFile(content, "text/csv", fileName)
  }
}
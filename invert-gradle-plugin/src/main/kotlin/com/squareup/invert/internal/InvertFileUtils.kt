package com.squareup.invert.internal

import java.io.File

object InvertFileUtils {
  fun String.addSlashAnd(str: String): String = buildString {
    append(this@addSlashAnd)
    append(File.separator)
    append(str)
  }

  const val REPORTS_FOLDER_NAME = "reports"
  const val INVERT_FOLDER_NAME = "invert"
  const val JS_FOLDER_NAME = "js"
  const val JSON_FOLDER_NAME = "json"
  const val SARIF_FOLDER_NAME = "sarif"

  val REPORTS_SLASH_INVERT_PATH = REPORTS_FOLDER_NAME.addSlashAnd(INVERT_FOLDER_NAME)

  fun outputFile(
    directory: File,
    filename: String,
  ): File {
    return File(directory, filename)
      .apply {
        parentFile.apply {
          if (!exists()) {
            // Create the directory if it does not exist
            mkdirs()
          }
        }
      }
  }
}

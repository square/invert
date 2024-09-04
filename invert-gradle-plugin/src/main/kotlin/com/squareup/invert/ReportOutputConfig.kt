package com.squareup.invert

import java.io.File

/**
 * Provided to a [StatCollector] to allow for custom report artifacts.
 */
data class ReportOutputConfig(
  val invertReportDirectory: File
)
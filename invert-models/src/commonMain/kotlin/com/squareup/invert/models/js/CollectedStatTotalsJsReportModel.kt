package com.squareup.invert.models.js

import com.squareup.invert.models.StatMetadata
import kotlinx.serialization.Serializable

/**
 * Collected ownership representation for a single module
 *
 * Used by [InvertPluginFileKey.OWNERS]
 */
@Serializable
data class CollectedStatTotalsJsReportModel(
  val statTotals: Map<StatMetadata, Int>
)

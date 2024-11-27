package com.squareup.invert.models.js

import com.squareup.invert.models.StatKey
import com.squareup.invert.models.StatMetadata
import kotlinx.serialization.Serializable

@Serializable
data class CollectedStatTotalsJsReportModel(
  val statTotals: Map<StatKey, StatTotalAndMetadata>
)

@Serializable
data class StatTotalAndMetadata(
  val metadata: StatMetadata,
  val total: Int,
)

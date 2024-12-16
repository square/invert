package com.squareup.invert.models.js

import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.OwnerName
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
  val totalByOwner: Map<OwnerName, Int>,
)

@Serializable
data class HistoricalData(
  val reportMetadata: MetadataJsReportModel,
  val statTotalsAndMetadata: CollectedStatTotalsJsReportModel
)

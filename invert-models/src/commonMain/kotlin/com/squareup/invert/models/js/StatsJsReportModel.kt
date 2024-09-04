package com.squareup.invert.models.js

import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatKey
import com.squareup.invert.models.StatMetadata
import kotlinx.serialization.Serializable

/**
 * Model for [JsReportFileKey.STATS] for Invert Web Report
 */
@Serializable
data class StatsJsReportModel(
  val statInfos: Map<StatKey, StatMetadata>,
  val statsByModule: Map<ModulePath, Map<StatKey, Stat>>,
)

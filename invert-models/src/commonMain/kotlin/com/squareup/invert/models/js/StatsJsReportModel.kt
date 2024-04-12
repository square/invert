package com.squareup.invert.models.js

import com.squareup.invert.models.GradlePath
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatInfo
import com.squareup.invert.models.StatKey
import kotlinx.serialization.Serializable

/**
 * Model for [JsReportFileKey.STATS] for Invert Web Report
 */
@Serializable
data class StatsJsReportModel(
  val statInfos: Map<StatKey, StatInfo>,
  val statsByModule: Map<GradlePath, Map<StatKey, Stat>>,
)

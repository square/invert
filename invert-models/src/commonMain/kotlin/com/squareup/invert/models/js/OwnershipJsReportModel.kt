package com.squareup.invert.models.js

import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.OwnerInfo
import com.squareup.invert.models.OwnerName
import kotlinx.serialization.Serializable

/**
 * Model [JsReportFileKey.OWNERS] for Invert Web Report
 */
@Serializable
data class OwnershipJsReportModel(
  val teams: Set<OwnerName>,
  val modules: Map<ModulePath, OwnerName>
)

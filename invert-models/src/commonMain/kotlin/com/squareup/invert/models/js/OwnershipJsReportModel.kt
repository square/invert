package com.squareup.invert.models.js

import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.OrgName
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

@Serializable
data class AllOwners(
  val ownerToDetails: Map<OwnerName, OwnerDetails>
)

@Serializable
data class OwnerDetails(
  val orgName: OrgName? = null,
  val metadata: Map<ModulePath, OwnerName> = emptyMap()
)

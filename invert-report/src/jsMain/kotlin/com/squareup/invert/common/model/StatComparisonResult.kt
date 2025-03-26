package com.squareup.invert.common.model
import com.squareup.invert.models.OwnerName
import com.squareup.invert.models.StatMetadata

data class StatComparisonResult(
  val statMetadata: StatMetadata,
  val currentTotal: Int,
  val previousTotal: Int,
  val relativeTotalChange: Int,
  val ownerChanges: Map<OwnerName, RelativeChangeValues>
)

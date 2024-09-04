package com.squareup.invert.models

import kotlinx.serialization.Serializable


/**
 * Information about a specific stat type.
 */
@Serializable
data class StatMetadata(
  val key: StatKey,
  val description: String,
  val dataType: StatDataType,
  val category: String = "Stats",
  val extras: List<ExtraMetadata> = emptyList(),
)

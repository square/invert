package com.squareup.invert.models

import kotlinx.serialization.Serializable


@Serializable
data class ExtraMetadata(
  val key: ExtraKey,
  val type: ExtraDataType,
  val description: String,
)

/**
 * Information about a specific stat type.
 */
@Serializable
data class StatMetadata(
  val key: StatKey,
  val description: String,
  val dataType: StatDataType,
  val category: String = "Stats",
  val extraMetadata: List<ExtraMetadata> = emptyList(),
)

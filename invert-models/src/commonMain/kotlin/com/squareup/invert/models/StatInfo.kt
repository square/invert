package com.squareup.invert.models

import kotlinx.serialization.Serializable

/**
 * Information about a specific stat type.
 */
@Serializable
data class StatInfo(
  val name: String,
  val description: String,
  val statType: CollectedStatType,
)

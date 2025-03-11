package com.squareup.invert.models

import kotlinx.serialization.Serializable


/**
 * Information about a specific stat type.
 */
@Serializable
data class StatMetadata(
  /** The unique key for this [Stat]. */
  val key: StatKey,
  /** A short, human-readable description of this [Stat]. Use [documentation] field for detailed info. */
  val description: String,
  /** The data type of this [Stat]. */
  val dataType: StatDataType,
  /** This is used for grouping [Stat] by category. */
  val category: String = "Stats",
  /** Metadata for the registered key/value "extras" for this [Stat]. */
  val extras: List<ExtraMetadata> = emptyList(),
  /** Documentation on this [Stat] in Markdown format. */
  val documentation: Markdown? = null,
)

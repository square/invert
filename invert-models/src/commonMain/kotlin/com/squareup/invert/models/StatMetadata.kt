package com.squareup.invert.models

import kotlinx.serialization.Serializable


/**
 * Information about a specific stat type.
 */
@Serializable
data class StatMetadata(
  /** The unique key for this [Stat]. */
  val key: StatKey,
  /** A markdown, description of this [Stat] and why it's being collected. */
  val description: Markdown,
  /** The data type of this [Stat]. */
  val dataType: StatDataType,
  /** This is used for grouping [Stat] by category. */
  val category: String = "Stats",
  /** The title of the [Stat] to be displayed in the UI. */
  val title: String = description,
  /** Metadata for the registered key/value "extras" for this [Stat]. */
  val extras: List<ExtraMetadata> = emptyList(),
)

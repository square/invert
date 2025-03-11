package com.squareup.invert.models

import kotlinx.serialization.Serializable


/**
 * Information about a specific stat type.
 */
@Serializable
data class StatMetadata(
  val key: StatKey,
  val title: String,
  /**
   * Use this field to give context on what this [Stat] is and why it's important.
   */
  val dataType: StatDataType,
  val description: Markdown? = null,
  val category: String = "Stats",
  val extras: List<ExtraMetadata> = emptyList(),
)

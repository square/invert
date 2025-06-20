package com.squareup.invert.models

import kotlinx.serialization.Serializable


/**
 * Metadata for "extras"
 */
@Serializable
data class ExtraMetadata(
  val key: ExtraKey,
  val type: ExtraDataType,
  val description: String,
  // Whether this metadata field should be filterable in the code references view.
  val filterable: Boolean = false,
)

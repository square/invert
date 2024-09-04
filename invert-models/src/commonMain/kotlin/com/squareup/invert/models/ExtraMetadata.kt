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
)

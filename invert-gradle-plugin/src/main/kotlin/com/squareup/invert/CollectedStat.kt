package com.squareup.invert

import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatMetadata
import kotlinx.serialization.Serializable


/**
 * A Collected [Stat] and its associated [StatMetadata]
 */
@Serializable
data class CollectedStat(
  val metadata: StatMetadata,
  val stat: Stat?
)
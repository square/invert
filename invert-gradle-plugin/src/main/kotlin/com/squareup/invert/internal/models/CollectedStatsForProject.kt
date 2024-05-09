package com.squareup.invert.internal.models

import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatInfo
import com.squareup.invert.models.StatKey
import kotlinx.serialization.Serializable

/**
 * Intermediate artifact holding Collected Stats for a Single Project
 */
@Serializable
data class CollectedStatsForProject(
  val path: String,
  val statInfos: Map<StatKey, StatInfo>,
  val stats: Map<StatKey, Stat>
)

package com.squareup.invert.internal.models

import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatKey
import com.squareup.invert.models.StatMetadata
import kotlinx.serialization.Serializable

/**
 * Intermediate artifact holding Collected Stats for a Single Project
 */
@Serializable
data class CollectedStatsForProject(
    val path: ModulePath,
    val statInfos: Map<StatKey, StatMetadata>,
    val stats: Map<StatKey, Stat>
)

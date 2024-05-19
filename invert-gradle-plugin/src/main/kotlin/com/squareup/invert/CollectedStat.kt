package com.squareup.invert

import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatMetadata


/**
 * A Collected [Stat] and its associated [StatMetadata]
 */
data class CollectedStat(
    val metadata: StatMetadata,
    val stat: Stat?
)
package com.squareup.invert

import com.squareup.invert.models.ModulePath

data class CollectedStatsAggregate(
  val aggregatedStatsByProject: Map<ModulePath, List<CollectedStat>>,
  val globalStats: List<CollectedStat>
)
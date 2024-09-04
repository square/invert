package com.squareup.invert

import com.squareup.invert.models.GradlePath

data class CollectedStatsAggregate(
  val aggregatedStatsByProject: Map<GradlePath, List<CollectedStat>>,
  val globalStats: List<CollectedStat>
)
package com.squareup.invert

import com.squareup.invert.models.GradlePath

data class CollectedStatsAggregate(
  val projectStats: Map<GradlePath, List<CollectedStat>>,
  val globalStats: List<CollectedStat>
)
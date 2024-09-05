package com.squareup.invert

import org.gradle.api.Named


/**
 * Interface representing all Invert [StatCollector]s.
 *
 * We must extend the [Named] interface to be used as a task input.
 */
interface StatCollector : Named {

  /**
   * Collect [CollectedStat]s for a specific project.
   */
  fun collect(
    invertProjectData: InvertProjectData,
  ): List<CollectedStat>? = null

  /**
   * Compute [CollectedStat]s with the context of all collected information.
   */
  fun aggregate(
    reportOutputConfig: ReportOutputConfig,
    invertAllCollectedDataRepo: InvertAllCollectedDataRepo,
  ): CollectedStatsAggregate? = null
}
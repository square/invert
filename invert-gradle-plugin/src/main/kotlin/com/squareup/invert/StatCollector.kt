package com.squareup.invert

import org.gradle.api.Named


/**
 * Interface representing all Invert [StatCollector]s.
 *
 * We must extend the [Named] interface to be used as a task input.
 */
interface StatCollector : Named {

  fun collect(
    invertProjectData: InvertProjectData,
  ): List<CollectedStat>?

}

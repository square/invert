package com.squareup.invert

import org.gradle.api.Project

interface InvertIncludeSubProjectCalculator {

  /**
   * Whether or not a subproject should be included in the `invert` plugin's analysis.
   *
   * @param subproject [Project] to be included
   * @return whether the [Project] should be included by [InvertGradlePlugin]
   */
  fun invoke(
    subproject: Project,
  ): Boolean
}

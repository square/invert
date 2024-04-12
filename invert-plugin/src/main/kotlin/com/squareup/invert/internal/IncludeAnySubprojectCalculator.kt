package com.squareup.invert.internal

import com.squareup.invert.InvertIncludeSubProjectCalculator
import org.gradle.api.Project

/**
 * Default implementation which will include any subproject
 */
object IncludeAnySubprojectCalculator : InvertIncludeSubProjectCalculator {
  override fun invoke(
    subproject: Project,
  ): Boolean {
    return true
  }
}

package com.squareup.invert

import com.squareup.invert.models.GradlePath
import com.squareup.invert.models.OwnerInfo

/**
 * Allows a user of Invert to specify how ownership is calculated.
 */
interface InvertOwnershipCollector {
  fun collect(
    rootProjectDir: String,
    gradlePath: GradlePath
  ): OwnerInfo?
}

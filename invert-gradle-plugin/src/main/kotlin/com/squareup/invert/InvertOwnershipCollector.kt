package com.squareup.invert

import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.OwnerInfo

/**
 * Allows a user of Invert to specify how ownership is calculated.
 */
interface InvertOwnershipCollector {
  fun collect(
    rootProjectDir: String,
    modulePath: ModulePath
  ): OwnerInfo?
}

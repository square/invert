package com.squareup.invert.internal

import com.squareup.invert.InvertOwnershipCollector
import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.OwnerInfo

/**
 * Default implementation of [InvertOwnershipCollector] which provides no ownership.
 */
object NoOpOwnershipCollector : InvertOwnershipCollector {
  override fun collect(
    rootProjectDir: String,
    modulePath: ModulePath
  ): OwnerInfo = OwnerInfo(OwnerInfo.UNOWNED)
}

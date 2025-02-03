package com.squareup.invert.internal

import com.squareup.invert.InvertOwnershipCollector
import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.OwnerInfo
import java.io.File

/**
 * Default implementation of [InvertOwnershipCollector] which provides no ownership.
 */
object NoOpOwnershipCollector : InvertOwnershipCollector() {

  override fun collect(
    rootProjectDir: File,
    modulePath: ModulePath
  ): OwnerInfo = OwnerInfo(OwnerInfo.UNOWNED)
}

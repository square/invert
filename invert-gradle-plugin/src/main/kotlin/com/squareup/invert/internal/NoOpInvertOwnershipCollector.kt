package com.squareup.invert.internal

import com.squareup.invert.InvertOwnershipCollector
import com.squareup.invert.models.OwnerInfo
import com.squareup.invert.models.OwnerName
import com.squareup.invert.models.js.AllOwners
import com.squareup.invert.models.js.OwnerDetails
import java.io.File

/**
 * Default implementation of [InvertOwnershipCollector] which provides no ownership.
 */
object NoOpInvertOwnershipCollector : InvertOwnershipCollector {
  override fun collect(
    gitRootDir: File,
    fileWithOwnership: File
  ): OwnerName = OwnerInfo.UNOWNED

  override fun collectAllOwners(
    gitRootDir: File,
  ): AllOwners {
    return AllOwners(mapOf(OwnerInfo.UNOWNED to OwnerDetails(null, emptyMap())))
  }
}

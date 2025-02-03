package com.squareup.invert

import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.OwnerInfo
import com.squareup.invert.models.OwnerName
import java.io.File

/**
 * Allows a user of Invert to specify how ownership is calculated.
 */
abstract class InvertOwnershipCollector {
  abstract fun collect(
    rootProjectDir: File,
    modulePath: ModulePath
  ): OwnerInfo

  open fun getOwnerNameForFile(
    rootProjectDir: File,
    fileInProject: File
  ): OwnerName {
    return OwnerInfo.UNOWNED
  }
}

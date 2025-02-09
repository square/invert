package com.squareup.invert

import com.squareup.invert.models.OwnerName
import java.io.File

/**
 * Allows a user of Invert to specify how ownership is calculated.
 */
interface InvertOwnershipCollector {

  fun collect(
    gitRootDir: File,
    fileWithOwnership: File
  ): OwnerName
}

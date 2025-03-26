package com.squareup.invert

import com.squareup.invert.models.OwnerName
import com.squareup.invert.models.js.AllOwners
import java.io.File

/**
 * Allows a user of Invert to specify how ownership is calculated.
 */
interface InvertOwnershipCollector {

  /**
   * Collects the owner of a file.
   */
  fun collect(
    gitRootDir: File,
    fileWithOwnership: File
  ): OwnerName

  /**
   * Collects all available owners.
   */
  fun collectAllOwners(
    gitRootDir: File,
  ): AllOwners
}

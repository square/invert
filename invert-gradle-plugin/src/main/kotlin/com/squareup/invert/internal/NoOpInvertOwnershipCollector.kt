package com.squareup.invert.internal

import com.squareup.invert.InvertOwnershipCollector
import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.OwnerName
import java.io.File

/**
 * Default implementation of [InvertOwnershipCollector] which provides no ownership.
 */
object NoOpInvertOwnershipCollector : InvertOwnershipCollector {

  override fun collect(
    rootProjectDir: File,
    modulePath: ModulePath
  ): OwnerName = "NoOp Invert Ownership"

  override fun getOwnerNameForFile(
    rootProjectDir: File,
    fileInProject: File
  ): OwnerName = "NoOp Invert Ownership"
}

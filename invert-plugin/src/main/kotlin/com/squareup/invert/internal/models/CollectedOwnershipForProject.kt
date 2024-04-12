package com.squareup.invert.internal.models

import com.squareup.invert.models.GradlePath
import com.squareup.invert.models.OwnerInfo
import kotlinx.serialization.Serializable

/**
 * Collected ownership representation for a single module
 *
 * Used by [InvertPluginFileKey.OWNERS]
 */
@Serializable
data class CollectedOwnershipForProject(
  val path: GradlePath,
  val ownerInfo: OwnerInfo,
)

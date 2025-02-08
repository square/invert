package com.squareup.invert.internal.models

import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.OwnerName
import kotlinx.serialization.Serializable

/**
 * Collected ownership representation for a single module
 *
 * Used by [InvertPluginFileKey.OWNERS]
 */
@Serializable
data class CollectedOwnershipForProject(
  val path: ModulePath,
  val ownerName: OwnerName,
)

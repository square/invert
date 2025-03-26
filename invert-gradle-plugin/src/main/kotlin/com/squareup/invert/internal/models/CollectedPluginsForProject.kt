package com.squareup.invert.internal.models

import com.squareup.invert.models.GradlePluginId
import com.squareup.invert.models.ModulePath
import kotlinx.serialization.Serializable

/**
 * Used by [InvertPluginFileKey.PLUGINS]
 */
@Serializable
data class CollectedPluginsForProject(
  val path: ModulePath,
  val plugins: List<GradlePluginId>,
)

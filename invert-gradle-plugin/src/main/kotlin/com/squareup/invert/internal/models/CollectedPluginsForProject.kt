package com.squareup.invert.internal.models

import com.squareup.invert.models.GradlePath
import com.squareup.invert.models.GradlePluginId
import kotlinx.serialization.Serializable

/**
 * Used by [InvertPluginFileKey.PLUGINS]
 */
@Serializable
data class CollectedPluginsForProject(
  val path: GradlePath,
  val plugins: List<GradlePluginId>,
)

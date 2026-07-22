package com.squareup.invert.internal.report

import com.squareup.invert.internal.models.CollectedConfigurationsForProject
import com.squareup.invert.internal.models.CollectedDependenciesForProject
import com.squareup.invert.internal.models.CollectedOwnershipForProject
import com.squareup.invert.internal.models.CollectedPluginsForProject
import com.squareup.invert.internal.models.CollectedStatsForProject
import kotlinx.serialization.DeserializationStrategy
import java.io.File

object InvertReportFileUtils {

  /**
   * Reads all files in the intermediate stats directory and combines their contents.
   */
  fun readCollectedStatsForAllProjectsFromDisk(
    collectedStatFile: File
  ): CollectedStatsForProject? = readIfPresent(
    file = collectedStatFile,
    deserializer = CollectedStatsForProject.serializer(),
  )

  fun readCollectedOwnershipForAllProjectsFromDisk(
    ownersFile: File
  ): CollectedOwnershipForProject? = readIfPresent(
    file = ownersFile,
    deserializer = CollectedOwnershipForProject.serializer(),
  )

  fun readCollectedPluginsForAllModules(
    pluginsFile: File
  ): CollectedPluginsForProject? = readIfPresent(
    file = pluginsFile,
    deserializer = CollectedPluginsForProject.serializer(),
  )

  fun buildModuleToFeaturesMap(
    dependenciesFile: File
  ): CollectedDependenciesForProject? = readIfPresent(
    file = dependenciesFile,
    deserializer = CollectedDependenciesForProject.serializer(),
  )

  fun readCollectedConfigurationsForAllModules(
    collectedConfigurationsFile: File
  ): CollectedConfigurationsForProject? = readIfPresent(
    file = collectedConfigurationsFile,
    deserializer = CollectedConfigurationsForProject.serializer(),
  )

  private fun <T> readIfPresent(
    file: File,
    deserializer: DeserializationStrategy<T>,
  ): T? {
    return if (file.exists() && file.nameWithoutExtension.isNotBlank()) {
      InvertJsonStreams.read(file, deserializer)
    } else {
      null
    }
  }
}

package com.squareup.invert.internal.report

import com.squareup.invert.internal.models.CollectedConfigurationsForProject
import com.squareup.invert.internal.models.CollectedDependenciesForProject
import com.squareup.invert.internal.models.CollectedOwnershipForProject
import com.squareup.invert.internal.models.CollectedPluginsForProject
import com.squareup.invert.internal.models.CollectedStatsForProject
import com.squareup.invert.models.InvertSerialization.InvertJson
import java.io.File

object InvertReportFileUtils {

  /**
   * Reads all files in the intermediate stats directory and combines their contents.
   */
  fun readCollectedStatsForAllProjectsFromDisk(
    collectedStatFiles: List<File>
  ): List<CollectedStatsForProject> {
    val collectedStats: List<CollectedStatsForProject> = collectedStatFiles
      .filter { it.exists() && it.nameWithoutExtension.isNotBlank() }
      .map {
        InvertJson.decodeFromString(CollectedStatsForProject.serializer(), it.readText())
      }
    return collectedStats
  }

  fun readCollectedOwnershipForAllProjectsFromDisk(
    ownersFiles: List<File>
  ): List<CollectedOwnershipForProject> {
    val collectedStats: List<CollectedOwnershipForProject> = ownersFiles
      .filter { it.exists() && it.nameWithoutExtension.isNotBlank() }
      .map {
        InvertJson.decodeFromString(
          deserializer = CollectedOwnershipForProject.serializer(),
          string = it.readText()
        )
      }
    return collectedStats
  }

  fun readCollectedPluginsForAllModules(pluginsFiles: List<File>): List<CollectedPluginsForProject> {
    return pluginsFiles
      .filter { it.exists() && it.nameWithoutExtension.isNotBlank() }
      .map {
        InvertJson.decodeFromString(
          deserializer = CollectedPluginsForProject.serializer(),
          string = it.readText()
        )
      }
  }

  fun buildModuleToFeaturesMap(dependenciesFiles: List<File>): List<CollectedDependenciesForProject> {
    return dependenciesFiles
      .filter { it.exists() && it.nameWithoutExtension.isNotBlank() }
      .map {
        InvertJson.decodeFromString(
          deserializer = CollectedDependenciesForProject.serializer(),
          string = it.readText()
        )
      }
  }

  fun readCollectedConfigurationsForAllModules(
    collectedConfigurationsFiles: List<File>
  ): List<CollectedConfigurationsForProject> {
    return collectedConfigurationsFiles
      .filter { it.exists() && it.nameWithoutExtension.isNotBlank() }
      .map {
        InvertJson.decodeFromString(
          deserializer = CollectedConfigurationsForProject.serializer(),
          string = it.readText()
        )
      }
  }
}

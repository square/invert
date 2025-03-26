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
    collectedStatFile: File
  ): CollectedStatsForProject? {
    return if (collectedStatFile.exists() && collectedStatFile.nameWithoutExtension.isNotBlank()) {
      InvertJson.decodeFromString(CollectedStatsForProject.serializer(), collectedStatFile.readText())
    } else {
      null
    }
  }

  fun readCollectedOwnershipForAllProjectsFromDisk(
    ownersFile: File
  ): CollectedOwnershipForProject? {
    return if (ownersFile.exists() && ownersFile.nameWithoutExtension.isNotBlank()) {
      InvertJson.decodeFromString(
        deserializer = CollectedOwnershipForProject.serializer(),
        string = ownersFile.readText()
      )
    } else {
      null
    }
  }

  fun readCollectedPluginsForAllModules(pluginsFile: File): CollectedPluginsForProject? {
    return if (pluginsFile.exists() && pluginsFile.nameWithoutExtension.isNotBlank()) {
      InvertJson.decodeFromString(
        deserializer = CollectedPluginsForProject.serializer(),
        string = pluginsFile.readText()
      )
    } else {
      null
    }
  }

  fun buildModuleToFeaturesMap(dependenciesFile: File): CollectedDependenciesForProject? {
    return if (dependenciesFile.exists() && dependenciesFile.nameWithoutExtension.isNotBlank()) {
      InvertJson.decodeFromString(
        deserializer = CollectedDependenciesForProject.serializer(),
        string = dependenciesFile.readText()
      )
    } else {
      null
    }
  }

  fun readCollectedConfigurationsForAllModules(
    collectedConfigurationsFile: File
  ): CollectedConfigurationsForProject? {
    return if (collectedConfigurationsFile.exists() && collectedConfigurationsFile.nameWithoutExtension.isNotBlank()) {
      InvertJson.decodeFromString(
        deserializer = CollectedConfigurationsForProject.serializer(),
        string = collectedConfigurationsFile.readText()
      )
    } else {
      null
    }
  }
}

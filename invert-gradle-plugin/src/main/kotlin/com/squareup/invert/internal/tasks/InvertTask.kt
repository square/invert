package com.squareup.invert.internal.tasks

import com.squareup.invert.internal.InvertFileUtils
import com.squareup.invert.internal.isRootProject
import com.squareup.invert.internal.models.InvertPluginFileKey
import com.squareup.invert.internal.report.InvertReportFileUtils
import com.squareup.invert.internal.report.js.InvertJsReportUtils
import com.squareup.invert.internal.report.js.InvertJsReportWriter
import com.squareup.invert.internal.report.json.InvertJsonReportWriter
import com.squareup.invert.models.DependencyId
import com.squareup.invert.models.GradlePath
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * This task orchestrates analysis on submodules and the result creates 2 formats:
 * 1. HTML/JS report
 * 2. JSON Artifacts that contain all data collected about submodules.
 */
abstract class InvertTask : DefaultTask() {

  companion object {
    const val TASK_NAME = "invert"
  }

  @get:Input
  abstract val forRootProject: Property<Boolean>

  @get:Input
  abstract val projectPath: Property<String>

  @get:InputFiles
  abstract val subprojectInvertReportDirs: ListProperty<Provider<Directory>>

  @get:OutputDirectory
  abstract val rootBuildReportsDir: DirectoryProperty

  @TaskAction
  internal fun execute() {
    val configurationsFiles = mutableListOf<File>()
    val dependenciesFiles = mutableListOf<File>()
    val ownersFiles = mutableListOf<File>()
    val statsFiles = mutableListOf<File>()
    val pluginsFiles = mutableListOf<File>()

    subprojectInvertReportDirs.get().forEach { subprojectInvertReportDir ->
      val subprojectInvertReportDirFile = subprojectInvertReportDir.get().asFile
      if (subprojectInvertReportDirFile.exists()) {
        File(
          subprojectInvertReportDirFile,
          InvertPluginFileKey.DEPENDENCIES.filename
        ).also {
          if (it.exists()) {
            dependenciesFiles.add(it)
          }
        }

        File(
          subprojectInvertReportDirFile,
          InvertPluginFileKey.CONFIGURATIONS.filename
        ).also {
          if (it.exists()) {
            configurationsFiles.add(it)
          }
        }

        File(
          subprojectInvertReportDirFile,
          InvertPluginFileKey.STATS.filename
        ).also {
          if (it.exists()) {
            statsFiles.add(it)
          }
        }

        File(
          subprojectInvertReportDirFile,
          InvertPluginFileKey.OWNERS.filename
        ).also {
          if (it.exists()) {
            ownersFiles.add(it)
          }
        }

        File(
          subprojectInvertReportDirFile,
          InvertPluginFileKey.PLUGINS.filename
        ).also {
          if (it.exists()) {
            pluginsFiles.add(it)
          }
        }
      }
    }

    val allProjectsDependencyData = InvertReportFileUtils.buildModuleToFeaturesMap(dependenciesFiles)
    val allProjectsConfigurationsData = InvertReportFileUtils.readCollectedConfigurationsForAllModules(
      configurationsFiles
    )
    val collectedStats = InvertReportFileUtils.readCollectedStatsForAllProjectsFromDisk(statsFiles)
    val allProjectsStatsData = InvertJsReportUtils.buildModuleToStatsMap(collectedStats)
    val allOwnersData = InvertReportFileUtils.readCollectedOwnershipForAllProjectsFromDisk(ownersFiles)
    val collectedOwnershipInfo = InvertJsReportUtils.buildModuleToOwnerMap(allOwnersData)
    val allPluginsData = InvertReportFileUtils.readCollectedPluginsForAllModules(pluginsFiles)

    val directDependenciesJsReportModel =
      InvertJsReportUtils.toDirectDependenciesJsReportModel(allProjectsDependencyData)

    val invertedDependenciesJsReportModel =
      InvertJsReportUtils.toInvertedDependenciesJsReportModel(allProjectsDependencyData)
    assertModuleMatch(
      logger = logger,
      modulesList = allProjectsDependencyData.map { it.path },
      invertedModulesList = invertedDependenciesJsReportModel.getAllModulePaths()
    )

    val rootBuildReportsDir = rootBuildReportsDir.get().asFile

    // JSON Report
    InvertJsonReportWriter(logger, rootBuildReportsDir).createInvertJsonReport(
      allConfigurationsData = allProjectsConfigurationsData,
      allProjectsDependencyData = allProjectsDependencyData,
      allProjectsStatsData = allProjectsStatsData,
      allPluginsData = allPluginsData,
      allOwnersData = allOwnersData,
    )

    // HTML/JS Report
    InvertJsReportWriter(logger, rootBuildReportsDir).createInvertHtmlReport(
      allProjectsDependencyData = allProjectsDependencyData,
      allProjectsStatsData = allProjectsStatsData,
      directDependencies = directDependenciesJsReportModel,
      invertedDependencies = invertedDependenciesJsReportModel,
      allPluginsData = allPluginsData,
      collectedOwnershipInfo = collectedOwnershipInfo,
      allProjectsConfigurationsData = allProjectsConfigurationsData,
    )
  }

  fun setParams(
    project: Project,
    subprojectInvertReportDirs: List<Provider<Directory>>,
  ) {
    this.subprojectInvertReportDirs.set(subprojectInvertReportDirs)
    this.forRootProject.set(project.isRootProject())
    this.projectPath.set(project.path)
    this.rootBuildReportsDir.set(
      project.layout.buildDirectory.dir(
        InvertFileUtils.REPORTS_SLASH_INVERT_PATH
      )
    )
  }

  /**
   * This provides a warning to the user to let them know that a module was found as a dependency
   * but was not scanned itself.  In order to get a full picture of the project, all should
   * be scanned.
   */
  private fun assertModuleMatch(
    logger: Logger,
    modulesList: List<GradlePath>,
    invertedModulesList: List<DependencyId>
  ) {
    if (!invertedModulesList.containsAll(modulesList)) {
      val modulesMap = modulesList.groupBy { it }
      val invertedModulesMap = invertedModulesList.groupBy { it }
      val errorString = buildString {
        appendLine("WARNING: Module Mismatch...")
        appendLine("The following modules are dependencies, but were not scanned:")
        var idx = 1
        invertedModulesMap.keys.sorted().forEach { path ->
          if (modulesMap[path] == null) {
            appendLine("${idx++}. $path")
          }
        }
      }

      logger.warn(errorString)
    }
  }
}

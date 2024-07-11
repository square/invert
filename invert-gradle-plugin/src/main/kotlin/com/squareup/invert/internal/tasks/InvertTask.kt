package com.squareup.invert.internal.tasks

import com.squareup.invert.internal.InvertFileUtils
import com.squareup.invert.internal.isRootProject
import com.squareup.invert.internal.models.CollectedConfigurationsForProject
import com.squareup.invert.internal.models.CollectedDependenciesForProject
import com.squareup.invert.internal.models.CollectedOwnershipForProject
import com.squareup.invert.internal.models.CollectedPluginsForProject
import com.squareup.invert.internal.models.CollectedStatsForProject
import com.squareup.invert.internal.models.InvertPluginFileKey
import com.squareup.invert.internal.report.InvertReportFileUtils
import com.squareup.invert.internal.report.js.InvertJsReportUtils
import com.squareup.invert.internal.report.js.InvertJsReportUtils.computeGlobalStats
import com.squareup.invert.internal.report.js.InvertJsReportWriter
import com.squareup.invert.internal.report.json.InvertJsonReportWriter
import com.squareup.invert.logging.GradleInvertLogger
import com.squareup.invert.logging.InvertLogger
import com.squareup.invert.models.DependencyId
import com.squareup.invert.models.GradlePath
import com.squareup.invert.models.js.CollectedStatTotalsJsReportModel
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
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

    @get:Input
    abstract val subprojectInvertReportDirs: ListProperty<String>

    @get:OutputDirectory
    abstract val rootBuildReportsDir: DirectoryProperty

    val invertLogger: InvertLogger by lazy { GradleInvertLogger(logger) }

    @TaskAction
    internal fun execute() {
        runBlocking {
            val collectedConfigurations = mutableListOf<CollectedConfigurationsForProject>()
            val collectedDependencies = mutableListOf<CollectedDependenciesForProject>()
            val collectedOwners = mutableListOf<CollectedOwnershipForProject>()
            val collectedStats = mutableListOf<CollectedStatsForProject>()
            val collectedPlugins = mutableListOf<CollectedPluginsForProject>()

            subprojectInvertReportDirs.get()
                .map { File(it) }
                .forEach { subprojectInvertReportDirFile ->
                    if (subprojectInvertReportDirFile.exists()) {
                        File(
                            subprojectInvertReportDirFile,
                            InvertPluginFileKey.DEPENDENCIES.filename
                        ).also { file ->
                            if (file.exists()) {
                                InvertReportFileUtils.buildModuleToFeaturesMap(file)?.let {
                                    collectedDependencies.add(it)
                                }
                            }
                        }

                        File(
                            subprojectInvertReportDirFile,
                            InvertPluginFileKey.CONFIGURATIONS.filename
                        ).also { file ->
                            if (file.exists()) {
                                InvertReportFileUtils.readCollectedConfigurationsForAllModules(file)?.let {
                                    collectedConfigurations.add(it)
                                }
                            }
                        }

                        File(
                            subprojectInvertReportDirFile,
                            InvertPluginFileKey.STATS.filename
                        ).also { file ->
                            if (file.exists()) {
                                InvertReportFileUtils.readCollectedStatsForAllProjectsFromDisk(file)?.let {
                                    collectedStats.add(it)
                                }
                            }
                        }

                        File(
                            subprojectInvertReportDirFile,
                            InvertPluginFileKey.OWNERS.filename
                        ).also { file ->
                            InvertReportFileUtils.readCollectedOwnershipForAllProjectsFromDisk(file)
                                ?.let { collectedOwners.add(it) }
                        }

                        File(
                            subprojectInvertReportDirFile,
                            InvertPluginFileKey.PLUGINS.filename
                        ).also { file ->
                            if (file.exists()) {
                                InvertReportFileUtils.readCollectedPluginsForAllModules(file)?.let {
                                    synchronized(collectedPlugins) {
                                        collectedPlugins.add(it)
                                    }
                                }
                            }
                        }
                    }
                }

            val collectedOwnershipInfo = InvertJsReportUtils.buildModuleToOwnerMap(collectedOwners)
            val allProjectsStatsData = InvertJsReportUtils.buildModuleToStatsMap(collectedStats)
            val directDependenciesJsReportModel =
                InvertJsReportUtils.toDirectDependenciesJsReportModel(collectedDependencies)
            val invertedDependenciesJsReportModel =
                InvertJsReportUtils.toInvertedDependenciesJsReportModel(collectedDependencies)

            assertModuleMatch(
                logger = invertLogger,
                modulesList = collectedDependencies.map { it.path },
                invertedModulesList = invertedDependenciesJsReportModel.getAllModulePaths()
            )

            val globalStats = computeGlobalStats(allProjectsStatsData)

            val rootBuildReportsDir = rootBuildReportsDir.get().asFile

            // JSON Report
            InvertJsonReportWriter(invertLogger, rootBuildReportsDir).createInvertJsonReport(
                allConfigurationsData = collectedConfigurations,
                allProjectsDependencyData = collectedDependencies,
                allProjectsStatsData = allProjectsStatsData,
                allPluginsData = collectedPlugins,
                allOwnersData = collectedOwners,
                globalStats = globalStats
            )

            // HTML/JS Report
            InvertJsReportWriter(invertLogger, rootBuildReportsDir).createInvertHtmlReport(
                allProjectsDependencyData = collectedDependencies,
                allProjectsStatsData = allProjectsStatsData,
                directDependencies = directDependenciesJsReportModel,
                invertedDependencies = invertedDependenciesJsReportModel,
                allPluginsData = collectedPlugins,
                collectedOwnershipInfo = collectedOwnershipInfo,
                allProjectsConfigurationsData = collectedConfigurations,
                globalStatTotals = CollectedStatTotalsJsReportModel(globalStats),
            )
        }
    }

    fun setParams(
        project: Project,
        subprojectInvertReportDirs: List<String>,
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
        logger: InvertLogger,
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

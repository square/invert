package com.squareup.invert.internal.tasks

import com.squareup.invert.InvertExtension
import com.squareup.invert.internal.InvertDependencyCollectors.computeCollectedDependenciesForProject
import com.squareup.invert.internal.InvertFileUtils
import com.squareup.invert.internal.InvertFileUtils.addSlashAnd
import com.squareup.invert.internal.getResolvedComponentResult
import com.squareup.invert.internal.isRootProject
import com.squareup.invert.internal.models.CollectedConfigurationsForProject
import com.squareup.invert.internal.models.CollectedDependenciesForProject
import com.squareup.invert.internal.models.InvertPluginFileKey
import com.squareup.invert.internal.projectConfigurations
import com.squareup.invert.internal.report.json.InvertJsonReportWriter
import com.squareup.invert.internal.toTransitiveDeps
import com.squareup.invert.logging.GradleInvertLogger
import com.squareup.invert.logging.InvertLogger
import com.squareup.invert.models.ConfigurationName
import com.squareup.invert.models.DependencyId
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependency
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Fully traverses the dependencies of a given Gradle Project and flattens the result.
 */
internal abstract class InvertCollectDependenciesTask : DefaultTask() {

    companion object {
        const val TASK_NAME = "invertCollectDependencies"

        /**
         * Resolve all configurations ending in "RuntimeClasspath"
         */
        private fun resolveMonitoredConfigurationsMap(
            filteredConfigurationNames: Collection<ConfigurationName>,
            project: Project,
        ): Map<ConfigurationName, Provider<ResolvedComponentResult>> {
            return mutableMapOf<ConfigurationName, Provider<ResolvedComponentResult>>()
                .also { resolvedConfigurationsMap ->
                    filteredConfigurationNames.forEach { monitoredConfigurationName: String ->
                        val resolvedComponentResultForConfiguration = project.projectConfigurations
                            .getResolvedComponentResult(monitoredConfigurationName)
                        if (resolvedComponentResultForConfiguration != null) {
                            resolvedConfigurationsMap[monitoredConfigurationName] =
                                resolvedComponentResultForConfiguration
                        }
                    }
                }
        }
    }

    @get:Input
    abstract val analyzedConfigurationNames: SetProperty<String>

    @get:Input
    abstract val allConfigurationNames: SetProperty<String>

    @get:Input
    abstract val projectPath: Property<String>

    @get:Input
    abstract val directDependencies: MapProperty<ConfigurationName, Set<DependencyId>>

    @get:Input
    abstract val monitoredConfigurationsMap: MapProperty<
            ConfigurationName,
            Provider<ResolvedComponentResult>
            >

    @get:OutputFile
    abstract val projectBuildReportDependenciesFile: RegularFileProperty

    @get:OutputFile
    abstract val projectBuildReportConfigurationsFile: RegularFileProperty

    private val invertLogger: InvertLogger by lazy { GradleInvertLogger(logger) }

    @TaskAction
    internal fun execute() {
        val computeCollectedFlattenedDependencyData = computeCollectedDependenciesForProject(
            directDependencies = this.directDependencies.get(),
            projectPath = projectPath.get(),
            transitiveDeps = monitoredConfigurationsMap.getOrElse(mapOf())
                .mapValues { it.value.get() }
                .toTransitiveDeps(),
        )
        val collectedConfigurations = CollectedConfigurationsForProject(
            path = projectPath.get(),
            allConfigurationNames = allConfigurationNames.get(),
            analyzedConfigurationNames = analyzedConfigurationNames.get(),
        )
        InvertJsonReportWriter.writeJsonFile(
            logger = invertLogger,
            jsonFileKey = InvertPluginFileKey.DEPENDENCIES,
            jsonOutputFile = projectBuildReportDependenciesFile.get().asFile,
            serializer = CollectedDependenciesForProject.serializer(),
            value = computeCollectedFlattenedDependencyData,
        )
        InvertJsonReportWriter.writeJsonFile(
            logger = invertLogger,
            jsonFileKey = InvertPluginFileKey.CONFIGURATIONS,
            jsonOutputFile = projectBuildReportConfigurationsFile.get().asFile,
            serializer = CollectedConfigurationsForProject.serializer(),
            value = collectedConfigurations,
        )
    }

    fun setParams(
        project: Project,
        extension: InvertExtension,
    ) {
        if (project.isRootProject()) {
            throw IllegalArgumentException("$TASK_NAME cannot be registered on a root project.")
        }

        projectPath.set(project.path)

        val allConfigurationNames: List<String> = project.projectConfigurations.map { it.name }
        this.allConfigurationNames.set(allConfigurationNames.toSortedSet())

        val filteredConfigurationNames = extension.getConfigurationsForProjectCalculator()
            .invoke(
                project = project,
                configurationNames = allConfigurationNames
            )
        this.analyzedConfigurationNames.set(filteredConfigurationNames.toSortedSet())

        val resolvedMonitoredConfigurationsMap = resolveMonitoredConfigurationsMap(
            filteredConfigurationNames = filteredConfigurationNames,
            project = project,
        )
        monitoredConfigurationsMap.set(resolvedMonitoredConfigurationsMap)

        val annotationProcessors = setOf("kapt", "ksp", "annotationProcessor")

        directDependencies.set(
            project.configurations
                .toSet()
                .filter {
                    filteredConfigurationNames.contains(it.name)
                            || annotationProcessors.contains(it.name)
                            || it.name.contains("kotlinCompilerPluginClasspath")
                }
                .associate { configuration ->
                    configuration.name to configuration.allDependencies.mapNotNull { dependency: Dependency ->
                        when (dependency) {
                            is ExternalDependency -> {
                                "${dependency.group}:${dependency.name}:${dependency.version}"
                            }

                            is DefaultProjectDependency -> {
                                dependency.dependencyProject.path
                            }

                            else -> {
                                null
                            }
                        }
                    }.toSortedSet()
                }
                .filter { it.value.isNotEmpty() }
        )

        projectBuildReportDependenciesFile.set(
            project.layout.buildDirectory.file(
                InvertFileUtils.REPORTS_SLASH_INVERT_PATH
                    .addSlashAnd(InvertPluginFileKey.DEPENDENCIES.filename)
            )
        )

        projectBuildReportConfigurationsFile.set(
            project.layout.buildDirectory.file(
                InvertFileUtils.REPORTS_SLASH_INVERT_PATH
                    .addSlashAnd(InvertPluginFileKey.CONFIGURATIONS.filename)
            )
        )
    }
}

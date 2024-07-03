package com.squareup.invert.internal.tasks

import com.squareup.invert.InvertExtension
import com.squareup.invert.StatCollector
import com.squareup.invert.internal.InvertFileUtils
import com.squareup.invert.internal.InvertFileUtils.addSlashAnd
import com.squareup.invert.internal.models.CollectedStatsForProject
import com.squareup.invert.internal.models.InvertPluginFileKey
import com.squareup.invert.internal.report.json.InvertJsonReportWriter
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatKey
import com.squareup.invert.models.StatMetadata
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * This task is responsible for Stat collection for Invert.
 */
internal abstract class InvertCollectStatsTask : DefaultTask() {

    companion object {
        const val TASK_NAME = "invertCollectStats"
    }

    @get:Input
    abstract val projectPath: Property<String>

    @get:Input
    abstract val rootProjectPath: Property<String>

    @get:Internal
    abstract var statCollectors: List<StatCollector>?

    @get:InputFiles
    abstract val projectMainSrcDirectory: DirectoryProperty

    @get:OutputFile
    abstract val projectBuildReportStatsFile: RegularFileProperty

    @TaskAction
    internal fun execute() {
        val projectPath = projectPath.get()

        // Only select files from main source (we don't want stats from tests).
        val mainSrcFolder = projectMainSrcDirectory.get().asFile
        val srcFolder = mainSrcFolder.parentFile.parentFile
        if (mainSrcFolder.exists()) {
            val kotlinSourceFiles = mainSrcFolder
                .walk()
                .filter { it.extension == "kt" }
                .toList()

            val statMetadataMap = mutableMapOf<StatKey, StatMetadata>()

            val collectedStats: Map<StatKey, Stat> =
                mutableMapOf<StatKey, Stat>().also { collectedStats ->
                    this.statCollectors?.forEach { statCollector ->
                        statCollector.collect(
                            rootProjectFolder = File(rootProjectPath.get()),
                            projectPath = projectPath,
                            kotlinSourceFiles = kotlinSourceFiles
                        )?.forEach { collectedStat ->
                            val statKey = collectedStat.metadata.key
                            collectedStat.stat?.let { stat ->
                                statMetadataMap[statKey] = collectedStat.metadata
                                collectedStats[statKey] = stat
                            }
                        }
                    }
                }

            InvertJsonReportWriter.writeJsonFile(
                logger,
                InvertPluginFileKey.STATS,
                this.projectBuildReportStatsFile.get().asFile,
                CollectedStatsForProject.serializer(),
                CollectedStatsForProject(
                    path = projectPath,
                    statInfos = statMetadataMap,
                    stats = collectedStats,
                )
            )
        }
    }

    fun setParams(
        project: Project,
        extension: InvertExtension,
    ) {
        val projectPath = project.path
        this.projectPath.set(projectPath)
        this.rootProjectPath.set(project.rootProject.layout.projectDirectory.asFile.absolutePath)
        this.projectMainSrcDirectory.set(
            File(project.layout.projectDirectory.asFile.path + "/src/main")
        )

        projectBuildReportStatsFile.set(
            project.layout.buildDirectory.file(
                InvertFileUtils.REPORTS_SLASH_INVERT_PATH.addSlashAnd(InvertPluginFileKey.STATS.filename)
            )
        )

        statCollectors = extension.getStatCollectors().toList()
    }
}

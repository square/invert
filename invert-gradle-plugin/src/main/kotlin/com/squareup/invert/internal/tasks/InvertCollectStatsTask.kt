package com.squareup.invert.internal.tasks

import com.squareup.invert.InvertExtension
import com.squareup.invert.InvertCollectContext
import com.squareup.invert.StatCollector
import com.squareup.invert.internal.InvertFileUtils
import com.squareup.invert.internal.InvertFileUtils.addSlashAnd
import com.squareup.invert.internal.models.CollectedStatsForProject
import com.squareup.invert.internal.models.InvertPluginFileKey
import com.squareup.invert.internal.report.json.InvertJsonReportWriter
import com.squareup.invert.logging.GradleInvertLogger
import com.squareup.invert.logging.InvertLogger
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
  abstract val projectGradlePath: Property<String>

  @get:Input
  abstract val rootProjectPath: Property<String>

  @get:Internal
  abstract var statCollectors: List<StatCollector>?

  @get:InputFiles
  abstract val projectSrcDirectory: DirectoryProperty

  @get:OutputFile
  abstract val projectBuildReportStatsFile: RegularFileProperty

  private fun invertLogger(): InvertLogger = GradleInvertLogger(logger)

  @TaskAction
  internal fun execute() {
    val projectPath = projectGradlePath.get()

    val projectDir = projectSrcDirectory.get().asFile.parentFile
    if (projectDir.exists()) {
      val statMetadataMap = mutableMapOf<StatKey, StatMetadata>()
      val collectedStats: Map<StatKey, Stat> = mutableMapOf<StatKey, Stat>()
        .also { collectedStats ->
          this.statCollectors?.forEach { statCollector ->
            statCollector.collect(
              InvertCollectContext(
                gitCloneDir = File(rootProjectPath.get()),
                modulePath = projectPath,
                moduleDir = projectDir,
              )
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
        invertLogger(),
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
    val projectGradlePath = project.path
    this.projectGradlePath.set(projectGradlePath)
    this.rootProjectPath.set(project.rootProject.layout.projectDirectory.asFile.absolutePath)
    this.projectSrcDirectory.set(
      File(project.layout.projectDirectory.asFile.path + "/src")
    )

    projectBuildReportStatsFile.set(
      project.layout.buildDirectory.file(
        InvertFileUtils.REPORTS_SLASH_INVERT_PATH.addSlashAnd(InvertPluginFileKey.STATS.filename)
      )
    )

    statCollectors = extension.getStatCollectors().toList()
  }
}

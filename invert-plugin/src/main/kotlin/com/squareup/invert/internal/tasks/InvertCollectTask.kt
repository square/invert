package com.squareup.invert.internal.tasks

import com.squareup.invert.internal.InvertFileUtils
import com.squareup.invert.internal.InvertFileUtils.addSlashAnd
import com.squareup.invert.internal.isRootProject
import com.squareup.invert.internal.models.CollectedPluginsForProject
import com.squareup.invert.internal.models.InvertPluginFileKey
import com.squareup.invert.internal.report.json.InvertJsonReportWriter
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * This task aggregates all of the collected data from scanned modules and assembles
 * reports.  It relies on [InvertCollectTask] which is registered on subprojects to collect
 * the data first, and then this task reads it all in to create the combined reports.
 */
abstract class InvertCollectTask : DefaultTask() {

  companion object {
    const val TASK_NAME = "invertCollect"
  }

  @get:OutputFile
  abstract val projectBuildReportPluginsFile: RegularFileProperty

  @get:Input
  abstract val projectPath: Property<String>

  @get:Input
  abstract val pluginIds: ListProperty<String>

  @TaskAction
  internal fun execute() {
    // Write Plugins for Project
    InvertJsonReportWriter.writeJsonFile(
      logger,
      InvertPluginFileKey.PLUGINS,
      projectBuildReportPluginsFile.get().asFile,
      CollectedPluginsForProject.serializer(),
      CollectedPluginsForProject(
        projectPath.get(),
        pluginIds.get()
      )
    )
  }

  fun setParams(
    project: Project,
  ) {
    if (project.isRootProject()) {
      throw IllegalArgumentException("This task cannot be applied to a root project.")
    }

    projectBuildReportPluginsFile.set(
      project.layout.buildDirectory.file(
        InvertFileUtils.REPORTS_SLASH_INVERT_PATH
          .addSlashAnd(InvertPluginFileKey.PLUGINS.filename)
      )
    )

    projectPath.set(project.path)

    pluginIds.set(
      project.plugins
        .map {
          it::class.java.name
        }
        .sorted()
    )
  }
}

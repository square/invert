package com.squareup.invert.internal.tasks

import com.squareup.invert.InvertGradlePlugin
import com.squareup.invert.internal.InvertFileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction

/**
 * Deletes all files generated from the [InvertGradlePlugin] Plugin
 */
internal abstract class InvertCleanTask : DefaultTask() {

  companion object {
    const val TASK_NAME = "invertClean"
  }

  @get:InputFiles
  abstract val projectBuildReportInvertDir: DirectoryProperty

  @TaskAction
  internal fun execute() {
    projectBuildReportInvertDir.get().asFile.deleteRecursively()
  }

  fun setParams(
    project: Project,
  ) {
    projectBuildReportInvertDir.set(
      project.layout.buildDirectory.dir(InvertFileUtils.REPORTS_SLASH_INVERT_PATH)
    )
  }
}

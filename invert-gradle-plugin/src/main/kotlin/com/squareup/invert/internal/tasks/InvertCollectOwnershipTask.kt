package com.squareup.invert.internal.tasks

import com.squareup.invert.InvertExtension
import com.squareup.invert.InvertOwnershipCollector
import com.squareup.invert.internal.InvertFileUtils
import com.squareup.invert.internal.InvertFileUtils.addSlashAnd
import com.squareup.invert.internal.isRootProject
import com.squareup.invert.internal.models.CollectedOwnershipForProject
import com.squareup.invert.internal.models.InvertPluginFileKey
import com.squareup.invert.internal.report.json.InvertJsonReportWriter
import com.squareup.invert.logging.GradleInvertLogger
import com.squareup.invert.logging.InvertLogger
import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.OwnerInfo
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Using the specified [InvertOwnershipCollector], it collects ownership info for a given module.
 */
internal abstract class InvertCollectOwnershipTask : DefaultTask() {
  companion object {
    const val TASK_NAME = "invertCollectOwnership"
  }

  @get:Input
  abstract val rootProjectDir: Property<String>

  @get:Input
  abstract val targetModule: Property<ModulePath>

  @get:OutputFile
  abstract val projectOwnershipJsonFile: RegularFileProperty

  @get:Internal
  abstract var ownershipCollector: InvertOwnershipCollector

  private fun invertLogger(): InvertLogger = GradleInvertLogger(logger)

  @TaskAction
  internal fun execute() {
    val collectedOwnerInfo: OwnerInfo? =
      ownershipCollector.collect(rootProjectDir.get(), targetModule.get())

    if (collectedOwnerInfo != null) {
      val projectOwnershipInfo = CollectedOwnershipForProject(
        path = targetModule.get(),
        ownerInfo = collectedOwnerInfo
      )

      InvertJsonReportWriter.writeJsonFile(
        logger = invertLogger(),
        jsonOutputFile = projectOwnershipJsonFile.get().asFile,
        jsonFileKey = InvertPluginFileKey.OWNERS,
        serializer = CollectedOwnershipForProject.serializer(),
        value = projectOwnershipInfo
      )
    } else {
      logger.info("Could not determine Ownership for Module ${this.targetModule}")
    }
  }

  fun setParams(
    project: Project,
    extension: InvertExtension,
  ) {
    if (project.isRootProject()) {
      throw IllegalStateException("This task cannot be applied to a root project.")
    }
    val projectLayout = project.layout
    val rootProjectLayout = project.rootProject.layout

    this.projectOwnershipJsonFile.set(
      projectLayout.buildDirectory.file(
        InvertFileUtils.REPORTS_SLASH_INVERT_PATH
          .addSlashAnd(InvertPluginFileKey.OWNERS.filename)
      )
    )

    this.ownershipCollector = extension.getOwnershipCollector()
    this.targetModule.set(project.path)
    this.rootProjectDir.value(rootProjectLayout.projectDirectory.asFile.path)
  }
}

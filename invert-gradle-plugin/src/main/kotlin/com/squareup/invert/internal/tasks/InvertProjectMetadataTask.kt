package com.squareup.invert.internal.tasks

import com.squareup.invert.internal.GitDataCollector
import com.squareup.invert.internal.InvertFileUtils
import com.squareup.invert.internal.InvertFileUtils.addSlashAnd
import com.squareup.invert.internal.isRootProject
import com.squareup.invert.internal.models.InvertPluginFileKey
import com.squareup.invert.internal.report.js.InvertJsReportWriter
import com.squareup.invert.internal.report.json.InvertJsonReportWriter
import com.squareup.invert.logging.GradleInvertLogger
import com.squareup.invert.logging.InvertLogger
import com.squareup.invert.models.GitBranch
import com.squareup.invert.models.js.JsReportFileKey
import com.squareup.invert.models.js.MetadataJsReportModel
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.UrlArtifactRepository
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.TimeZone

/**
 * This task collects project metadata related to Git.
 */
abstract class InvertProjectMetadataTask : DefaultTask() {

  private fun invertLogger(): InvertLogger = GradleInvertLogger(logger)

  companion object {
    const val TASK_NAME = "invertProjectMetadata"

    fun gatherProjectMetadata(
      logger: InvertLogger,
      gitProjectDir: File,
      timeZoneId: String = "America/New_York",
      datePatternFormat: String = "MMMM dd, yyyy[ 'at' HH:mm:ss]",
      repoUrls: List<String> = listOf(),
    ): MetadataJsReportModel {

      val time = Instant.now()
      val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(datePatternFormat)
        .withZone(TimeZone.getTimeZone(timeZoneId).toZoneId())

      val gitDataCollector = GitDataCollector(gitProjectDir)
      val currentBranch: GitBranch = gitDataCollector.currentBranch()
      val currentBranchHash = gitDataCollector.gitShaOfBranch(currentBranch, logger)

      val remoteGitRepoUrl = gitDataCollector.remoteGitRepoUrl()
      val remoteRepoUrl = if (remoteGitRepoUrl.endsWith(".git")) {
        GitDataCollector.remoteRepoGitUrlToHttps(
          remoteGitRepoUrl = remoteGitRepoUrl
        )
      } else {
        remoteGitRepoUrl
      }

      return MetadataJsReportModel(
        time = time.epochSecond,
        timeStr = formatter.format(time),
        gitSha = currentBranchHash,
        branchName = currentBranch,
        timezoneId = timeZoneId,
        currentBranch = currentBranch,
        currentBranchHash = currentBranchHash,
        remoteRepoGit = remoteGitRepoUrl,
        remoteRepoUrl = remoteRepoUrl,
        mavenRepoUrls = repoUrls,
      )
    }
  }

  @get:Input
  abstract val mavenRepoUrls: ListProperty<String>

  @get:OutputFile
  abstract val metadataJsFile: RegularFileProperty

  @get:OutputFile
  abstract val metadataJsonFile: RegularFileProperty

  @get:Input
  abstract val timeZoneId: Property<String>

  @get:Input
  abstract val datePatternFormat: Property<String>

  @TaskAction
  internal fun execute() {
    val timeZoneId = this.timeZoneId.get()
    val datePatternFormat = this.datePatternFormat.get()

    val reportMetadata = gatherProjectMetadata(
      timeZoneId = timeZoneId,
      datePatternFormat = datePatternFormat,
      logger = invertLogger(),
      repoUrls = this.mavenRepoUrls.get(),
      gitProjectDir = File("."), // TODO Pass in the root of the Git Repo
    )

    InvertJsReportWriter
      .writeJsFile(
        logger = invertLogger(),
        fileKey = JsReportFileKey.METADATA,
        jsOutputFile = this.metadataJsFile.get().asFile,
        serializer = MetadataJsReportModel.serializer(),
        value = reportMetadata,
      )

    InvertJsonReportWriter
      .writeJsonFile(
        logger = invertLogger(),
        jsonFileKey = InvertPluginFileKey.METADATA,
        jsonOutputFile = this.metadataJsonFile.get().asFile,
        serializer = MetadataJsReportModel.serializer(),
        value = reportMetadata,
      )
  }

  fun setParams(
    rootProject: Project,
  ) {
    if (!rootProject.isRootProject()) {
      throw IllegalStateException("This task can only be applied to a root project.")
    }
    val timeZoneId = "America/New_York"
    val PATTERN_FORMAT = "MMMM dd, yyyy[ 'at' HH:mm:ss]"

    this.timeZoneId.set(timeZoneId)
    this.datePatternFormat.set(PATTERN_FORMAT)

    this.metadataJsFile.set(
      rootProject.layout.buildDirectory.file(
        InvertFileUtils.REPORTS_SLASH_INVERT_PATH
          .addSlashAnd(InvertFileUtils.JS_FOLDER_NAME)
          .addSlashAnd(JsReportFileKey.METADATA.jsFilename)
      )
    )
    this.metadataJsonFile.set(
      rootProject.layout.buildDirectory.file(
        InvertFileUtils.REPORTS_SLASH_INVERT_PATH
          .addSlashAnd(InvertFileUtils.JSON_FOLDER_NAME)
          .addSlashAnd(InvertPluginFileKey.METADATA.filename)
      )
    )

    this.mavenRepoUrls.set(
      rootProject.buildscript.repositories
        .filterIsInstance<UrlArtifactRepository>()
        .map { it.url.toURL().toString() }
        .map {
          if (it.endsWith("/")) {
            it.dropLast(1)
          } else {
            it
          }
        }
        .distinct()
    )
  }
}

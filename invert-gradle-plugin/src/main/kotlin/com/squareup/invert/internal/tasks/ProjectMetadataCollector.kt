package com.squareup.invert.internal.tasks

import com.squareup.invert.internal.GitDataCollector
import com.squareup.invert.logging.InvertLogger
import com.squareup.invert.models.GitBranch
import com.squareup.invert.models.js.BuildSystem
import com.squareup.invert.models.js.MetadataJsReportModel
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.TimeZone

/**
 * This task collects project metadata related to Git.
 */
object ProjectMetadataCollector {

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
    val currentTag: GitBranch? = gitDataCollector.currentTag()
    val currentBranchHash = gitDataCollector.gitShaOfBranch(currentBranch, logger)

    val latestCommitTimestamp = gitDataCollector.latestCommitTimestamp()

    val remoteGitRepoUrl = gitDataCollector.remoteGitRepoUrl()
    val remoteRepoUrl = if (remoteGitRepoUrl.endsWith(".git")) {
      GitDataCollector.remoteRepoGitUrlToHttps(
        remoteGitRepoUrl = remoteGitRepoUrl
      )
    } else {
      remoteGitRepoUrl
    }

    val buildSystem = if (gitProjectDir.listFiles().any { it.name.contains(".gradle") }) {
      // Has a settings.gradle or settings.gradle.kts or build.gradle or build.gradle.kts file in the root
      BuildSystem.GRADLE
    } else {
      BuildSystem.OTHER
    }

    return MetadataJsReportModel(
      currentTime = time.epochSecond,
      currentTimeStr = formatter.format(time),
      currentTimezoneId = timeZoneId,
      latestCommitTime = latestCommitTimestamp,
      latestCommitTimeFormatted = formatter.format(Instant.ofEpochSecond(latestCommitTimestamp)),
      latestCommitGitSha = currentBranchHash,
      branchName = currentBranch,
      tagName = currentTag,
      latestCommitSha = currentBranchHash,
      remoteRepoGit = remoteGitRepoUrl,
      remoteRepoUrl = remoteRepoUrl,
      artifactRepositories = repoUrls,
      buildSystem = buildSystem,
    )
  }

}

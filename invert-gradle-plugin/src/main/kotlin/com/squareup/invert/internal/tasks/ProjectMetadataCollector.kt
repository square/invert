package com.squareup.invert.internal.tasks

import com.squareup.invert.internal.GitDataCollector
import com.squareup.invert.logging.InvertLogger
import com.squareup.invert.models.GitBranch
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

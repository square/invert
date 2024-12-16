package com.squareup.invert.models.js

import com.squareup.invert.models.GitBranch
import com.squareup.invert.models.GitSha
import com.squareup.invert.models.GitTag
import kotlinx.serialization.Serializable

/**
 * Model [JsReportFileKey.METADATA] for Invert Web Report
 */
@Serializable
data class MetadataJsReportModel(
  val artifactRepositories: List<String>,
  val branchName: GitBranch?,
  val buildSystem: BuildSystem,
  val currentTime: Long,
  val currentTimeFormatted: String,
  val latestCommitGitSha: GitSha,
  val latestCommitTime: Long,
  val latestCommitTimeFormatted: String,
  val tagName: GitTag?,
  val timezoneId: String,
  val remoteRepoGit: String,
  val remoteRepoUrl: String,
)

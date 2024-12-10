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
    val currentTime: Long,
    val currentTimeStr: String,
    val currentTimezoneId: String,
    val latestCommitSha: GitSha,
    val latestCommitTime: Long,
    val latestCommitTimeFormatted: String,
    val latestCommitGitSha: GitSha?,
    val branchName: GitBranch?,
    val tagName: GitTag?,
    val remoteRepoGit: String,
    val remoteRepoUrl: String,
    val artifactRepositories: List<String>,
    val buildSystem: BuildSystem,
)

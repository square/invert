package com.squareup.invert.models.js

import com.squareup.invert.models.GitBranch
import com.squareup.invert.models.GitSha
import kotlinx.serialization.Serializable

/**
 * Model [JsReportFileKey.METADATA] for Invert Web Report
 */
@Serializable
data class MetadataJsReportModel(
    val time: Long,
    val timezoneId: String,
    val timeStr: String,
    val gitSha: GitSha?,
    val branchName: GitBranch?,
    val currentBranch: String,
    val currentBranchHash: GitSha,
    val remoteRepoGit: String,
    val remoteRepoUrl: String,
    val mavenRepoUrls: List<String>
)

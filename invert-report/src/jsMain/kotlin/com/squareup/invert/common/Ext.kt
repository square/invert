package com.squareup.invert.common

import com.squareup.invert.models.js.MetadataJsReportModel

/** TODO fix in https://github.com/square/invert/issues/14 */
private fun String.plusExamplesForInvertRepo(): String = if (this.contains("square/invert")) {
  "$this/examples"
} else {
  this
}

fun MetadataJsReportModel.httpsUrlForBranch() = "${remoteRepoUrl}/tree/${branchName}".plusExamplesForInvertRepo()

fun MetadataJsReportModel.httpsUrlForCommit() = "${remoteRepoUrl}/blob/${gitSha}".plusExamplesForInvertRepo()

package com.squareup.invert.owners

import com.github.pgreze.kowners.OwnersResolver
import com.github.pgreze.kowners.findCodeOwnerLocations
import com.github.pgreze.kowners.parseCodeOwners
import com.squareup.invert.InvertOwnershipCollector
import com.squareup.invert.models.OwnerInfo
import com.squareup.invert.models.OwnerName
import com.squareup.invert.models.js.AllOwners
import com.squareup.invert.models.js.OwnerDetails
import java.io.File

object GitHubCodeOwnersInvertOwnershipCollector : InvertOwnershipCollector {

  fun getCodeownerFiles(gitRootDir: File): List<File> {
    val CODEOWNERS_FILES = gitRootDir.findCodeOwnerLocations()
    return CODEOWNERS_FILES
      .onEach { println(it.path) }
      .filter { it.exists() && it.isFile }
  }

  fun getOwnersResolver(gitRootDir: File): OwnersResolver? {
    val codeownerLines = getCodeownerFiles(gitRootDir).flatMap { file -> file.readLines() }

    return if (codeownerLines.isEmpty()) {
      OwnersResolver(codeownerLines.parseCodeOwners())
    } else {
      null
    }
  }

  override fun collect(
    gitRootDir: File,
    fileWithOwnership: File
  ): OwnerName {
    val ownersResolver = getOwnersResolver(gitRootDir)
    return if (ownersResolver != null) {
      val owners = ownersResolver.resolveOwnership(fileWithOwnership.relativeTo(gitRootDir).path)
      owners?.joinToString(",") ?: OwnerInfo.UNOWNED
    } else {
      OwnerInfo.UNOWNED
    }
  }

  override fun collectAllOwners(
    gitRootDir: File,
  ): AllOwners {
    val ownersResolver = getOwnersResolver(gitRootDir)
    val allOwners = ownersResolver?.ownerships?.map { it.owners }?.toSet() ?: emptySet()
    val ownerToDetails = allOwners.associate { it.joinToString(",") to OwnerDetails()  }
    return AllOwners(ownerToDetails = ownerToDetails)
  }
}
package com.squareup.invert.owners

import com.github.pgreze.kowners.OwnersResolver
import com.github.pgreze.kowners.findCodeOwnerLocations
import com.github.pgreze.kowners.findGitRootPath
import com.github.pgreze.kowners.parseCodeOwners
import com.squareup.invert.InvertOwnershipCollector
import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.OwnerInfo
import com.squareup.invert.models.OwnerName
import java.io.File

object GitHubCodeOwnersInvertOwnershipCollector : InvertOwnershipCollector {

  override fun collect(
    rootProjectDir: File,
    modulePath: ModulePath
  ): OwnerName {
    val moduleDir = File(rootProjectDir, modulePath.drop(1).replace(":", "/"))
    return getOwnerNameForFile(rootProjectDir, moduleDir)
  }

  override fun getOwnerNameForFile(
    rootProjectDir: File,
    fileInProject: File
  ): OwnerName {
    val gitRoot = rootProjectDir.findGitRootPath()
      ?: throw IllegalStateException("This is not a Git Repository.  Could not locate the .git folder at the root.")

    val CODEOWNERS_FILES = gitRoot.findCodeOwnerLocations()

    val allLines = mutableListOf<String>()
    CODEOWNERS_FILES
      .onEach { println(it.path) }
      .filter { it.exists() && it.isFile }
      .forEach { allLines.addAll(it.readLines()) }

    val ownersResolver by lazy {
      OwnersResolver(allLines.parseCodeOwners())
    }

    return if (allLines.isEmpty()) {
      "No CODEOWNERS File at ${CODEOWNERS_FILES.map { it.path }}"
    } else {
      // TODO FIX THIS COMPUTATION
      val owners = ownersResolver.resolveOwnership(fileInProject.relativeTo(rootProjectDir).path)
      owners?.joinToString("/n") ?: OwnerInfo.UNOWNED
    }
  }
}
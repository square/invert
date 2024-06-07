package com.squareup.invert

import com.github.pgreze.kowners.OwnersResolver
import com.github.pgreze.kowners.findCodeOwnerLocations
import com.github.pgreze.kowners.findGitRootPath
import com.github.pgreze.kowners.parseCodeOwners
import com.squareup.invert.models.GradlePath
import com.squareup.invert.models.OwnerInfo
import java.io.File

object GitHubCodeOwnersInvertOwnershipCollector : InvertOwnershipCollector {
    override fun collect(rootProjectDir: String, gradlePath: GradlePath): OwnerInfo? {

        val gitRoot = File(rootProjectDir).findGitRootPath()
            ?: throw IllegalStateException("This is not a Git Repository.  Could not locate the .git folder at the root.")

        val CODEOWNERS_FILES = gitRoot.findCodeOwnerLocations()

        val allLines = mutableListOf<String>()
        CODEOWNERS_FILES
            .onEach { println(it.path) }
            .filter { it.exists() && it.isFile }
            .forEach { allLines.addAll(it.readLines()) }

        val ownersResolver by lazy {
            OwnersResolver(
                allLines.parseCodeOwners()
            )
        }

        return OwnerInfo(
            if (allLines.isEmpty()) {
                "No CODEOWNERS File at ${CODEOWNERS_FILES.map { it.path }}"
            } else {
                // TODO FIX THIS COMPUTATION
                val projectDir = File(rootProjectDir, gradlePath.drop(1).replace(":", "/"))
                val relativePath = projectDir.absolutePath.replace(gitRoot.absolutePath, "")
                val owners = ownersResolver
                    .resolveOwnership(relativePath)
                owners?.joinToString("/n") ?: "NONE"
            }
        )
    }
}
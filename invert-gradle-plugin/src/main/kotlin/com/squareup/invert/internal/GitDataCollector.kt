package com.squareup.invert.internal

import com.squareup.invert.models.GitBranch
import com.squareup.invert.models.GitSha
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import java.io.File
import java.io.IOException

/**
 * Executes `git` commands and parses the output.
 */
internal class GitDataCollector(private val gitProjectRootDir: File) {

  companion object {
    private const val GIT_EXTENSION = ".git"

    /**
     * Takes a remote abc.git URL and attempts to create an https url.
     *
     * @param remoteGitRepoUrl Will look like:
     * org-123@github.com:organization/myrepo.git
     *
     * @return https address of Git Project.  For this example the result would be
     * https://github.com/organization/myrepo
     */
    fun remoteRepoGitUrlToHttps(remoteGitRepoUrl: String): String {
      return listOf(remoteGitRepoUrl).map {
        if (it.contains("@")) {
          it.split("@")[1]
        } else {
          it
        }
      }.map {
        if (it.endsWith(GIT_EXTENSION)) {
          it.substringBeforeLast(GIT_EXTENSION)
        } else {
          it
        }
      }.map {
        if (!it.startsWith("https://")) {
          buildString {
            append("https://")
            append(it.replace(":", "/"))
          }
        } else {
          it
        }
      }[0]
    }
  }

  fun currentBranch(): GitBranch {
    return exec("git rev-parse --abbrev-ref HEAD", gitProjectRootDir).stdOut.lines()[0]
  }

  /**
   *
   * Example output of the command is:
   * org-49461806@github.com:squareup/android-register.git
   */
  fun remoteGitRepoUrl(): String {
    return exec("git config --get remote.origin.url", gitProjectRootDir).stdOut.lines()[0]
  }

  fun gitShaOfBranch(branchName: GitBranch, logger: Logger): GitSha {
    val command = buildString {
      append("git log -n 1 ")
      append(branchName)
    }
    logger.info("Running: $command")

    val lines = try {
      exec(
        command = command,
        cwd = gitProjectRootDir
      ).stdOut.lines()
    } catch (e: IOException) {
      e.message?.lines() ?: emptyList()
    }

    if (lines.size < 2) {
      throw GradleException(
        """
            Couldn't calculate git sha hash of $branchName
        """.trimIndent()
      )
    }

    return parseGitHash(lines[0])
  }

  private fun parseGitHash(line: String): String {
    return line.split(" ")[1]
  }
}

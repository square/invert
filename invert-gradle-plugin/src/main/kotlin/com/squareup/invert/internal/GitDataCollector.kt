package com.squareup.invert.internal

import com.squareup.invert.models.GitBranch
import com.squareup.invert.models.GitSha
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import java.io.IOException

/**
 * Executes `git` commands and parses the output.
 */
internal object GitDataCollector {

  fun currentBranch(): GitBranch {
    return exec("git rev-parse --abbrev-ref HEAD").stdOut.lines()[0]
  }

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
      val gitExtension = ".git"
      if (it.endsWith(gitExtension)) {
        it.substring(0, it.length - gitExtension.length)
      } else {
        it
      }
    }.map {
      buildString {
        append("https://")
        append(it.replace(":", "/"))
      }
    }[0]
  }

  /**
   *
   * Example output of the command is:
   * org-49461806@github.com:squareup/android-register.git
   */
  fun remoteGitRepoUrl(): String {
    return exec("git config --get remote.origin.url").stdOut.lines()[0]
  }

  fun gitShaOfBranch(branchName: GitBranch, logger: Logger): GitSha {
    val command = buildString {
      append("git log -n 1 ")
      append(branchName)
    }
    logger.info("Running: $command")

    val lines = try {
      exec(command).stdOut.lines()
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

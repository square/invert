package com.squareup.invert.internal

import com.squareup.invert.internal.exec
import com.squareup.invert.models.GitBranch
import com.squareup.invert.models.GitSha
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals

/**
 * Executes `git` commands and parses the output.
 */
class GitDataCollectorTest {

  @Test
  fun testOrgAtGit() {
    val actual = GitDataCollector.remoteRepoGitUrlToHttps(
      "org-123@github.com:company/my-repo.git"
    )
    assertEquals(
      expected = "https://github.com/company/my-repo",
      actual = actual
    )
  }

  @Test
  fun testHttpsDotGit() {
    val actual = GitDataCollector.remoteRepoGitUrlToHttps(
      "git@github.com:square/invert.git"
    )
    assertEquals(
      expected = "https://github.com/square/invert",
      actual = actual
    )
  }


  @Test
  fun testHttps() {
    val actual = GitDataCollector.remoteRepoGitUrlToHttps(
      "https://github.com/company/my-repo"
    )
    assertEquals(
      expected = "https://github.com/company/my-repo",
      actual = actual
    )
  }
}

package com.squareup.invert.internal

import org.junit.Test
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

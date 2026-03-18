package com.squareup.invert

import com.squareup.invert.internal.models.InvertCombinedCollectedData
import com.squareup.invert.models.js.AllOwners
import com.squareup.invert.models.js.BuildSystem
import com.squareup.invert.models.js.MetadataJsReportModel
import com.squareup.invert.models.js.OwnerDetails
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class InvertAllCollectedDataRepoTest {

  @Test
  fun `getOwners returns owner details when metadata is populated`() {
    val owners = AllOwners(
      ownerToDetails = mapOf(
        "payments" to OwnerDetails(
          orgName = "Cash",
          metadata = mapOf(
            "display_name" to "Payments Platform",
            "contact" to "#payments-platform",
            "team_id" to "T123",
            "core_reviewer" to "jane",
            "engineering_manager" to "sam"
          )
        )
      )
    )
    val repo = createRepo(owners = owners)

    val result = repo.getOwners()

    assertEquals(1, result.ownerToDetails.size)
    val paymentsDetails = result.ownerToDetails.getValue("payments")
    assertEquals("Cash", paymentsDetails.orgName)
    assertEquals("Payments Platform", paymentsDetails.metadata["display_name"])
    assertEquals("#payments-platform", paymentsDetails.metadata["contact"])
    assertEquals("T123", paymentsDetails.metadata["team_id"])
    assertEquals("jane", paymentsDetails.metadata["core_reviewer"])
    assertEquals("sam", paymentsDetails.metadata["engineering_manager"])
  }

  @Test
  fun `getOwners returns empty map when metadata has no owners`() {
    val repo = createRepo(owners = AllOwners(ownerToDetails = emptyMap()))

    val result = repo.getOwners()

    assertTrue(result.ownerToDetails.isEmpty())
  }

  @Test
  fun `getOwners returns read-only stable snapshot for callers`() {
    val repo = createRepo(
      owners = AllOwners(
        ownerToDetails = mutableMapOf(
          "platform" to OwnerDetails(
            orgName = "Foundation",
            metadata = mutableMapOf("display_name" to "Platform Team")
          )
        )
      )
    )

    val firstRead = repo.getOwners()

    assertFailsWith<UnsupportedOperationException> {
      (firstRead.ownerToDetails as MutableMap<String, OwnerDetails>)["new-owner"] = OwnerDetails()
    }

    val firstDetails = firstRead.ownerToDetails.getValue("platform")
    assertFailsWith<UnsupportedOperationException> {
      (firstDetails.metadata as MutableMap<String, String>)["contact"] = "#platform"
    }

    val secondRead = repo.getOwners()
    assertEquals(firstRead, secondRead)
    assertEquals("Platform Team", secondRead.ownerToDetails.getValue("platform").metadata["display_name"])
  }

  private fun createRepo(owners: AllOwners): InvertAllCollectedDataRepo {
    return InvertAllCollectedDataRepo(
      allCollectedData = InvertCombinedCollectedData(
        collectedConfigurations = emptySet(),
        collectedDependencies = emptySet(),
        collectedOwners = emptySet(),
        collectedStats = emptySet(),
        collectedPlugins = emptySet()
      ),
      projectMetadata = MetadataJsReportModel(
        artifactRepositories = emptyList(),
        branchName = "main",
        buildSystem = BuildSystem.GRADLE,
        currentTime = 0L,
        currentTimeFormatted = "0",
        latestCommitGitSha = "abc123",
        latestCommitTime = 0L,
        latestCommitTimeFormatted = "0",
        tagName = null,
        timezoneId = "UTC",
        remoteRepoGit = "git@github.com:example/invert.git",
        remoteRepoUrl = "https://github.com/example/invert",
        owners = owners
      )
    )
  }
}

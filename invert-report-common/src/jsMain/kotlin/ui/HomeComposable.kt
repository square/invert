package ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.AllModulesNavRoute
import com.squareup.invert.common.navigation.routes.ArtifactsNavRoute
import com.squareup.invert.common.navigation.routes.OwnersNavRoute
import com.squareup.invert.common.navigation.routes.PluginsNavRoute
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.H5
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

@Composable
fun HomeComposable(reportDataRepo: ReportDataRepo, navRouteRepo: NavRouteRepo) {
  val moduleCount by reportDataRepo.allModules.map { it?.size }.collectAsState(null)
  val artifactCount by reportDataRepo.allArtifacts.map { it?.size }.collectAsState(null)
  val ownersCount by reportDataRepo.allOwnerNames.map { it?.size }.collectAsState(null)
  val pluginIdsCount by reportDataRepo.allPluginIds.map { it?.size }.collectAsState(null)
  val reportMetadata by reportDataRepo.reportMetadata.collectAsState(null)

  val metadata = reportMetadata
  BootstrapJumbotron({
    Text("Invert Report")
    Br { }
    if (metadata == null) {
      BootstrapLoadingSpinner()
    } else {
      H5 {
        Text("Collected on ${metadata.timeStr} (${metadata.timezoneId})")
      }
    }
  }, {
    if (metadata == null) {
      BootstrapLoadingSpinner()
    } else {
      Br {}
      P {
        Text("Repo ")
        A(href = metadata.remoteRepoUrl) {
          Text(metadata.remoteRepoUrl)
        }
      }
      metadata.gitSha?.let { gitSha ->
        P({
          classes("fs-6")
        }) {
          Text("Last Commit from ${metadata.branchName} ")
          val commitUrl = metadata.remoteRepoUrl + "/commits/" + gitSha
          A(href = commitUrl) {
            Text(gitSha)
          }
        }
        if (metadata.branchName != metadata.currentBranch) {
          P({
            classes("fs-6 text-warning bg-dark text-center".split(" "))
          }) {
            Text("Report was not run on ${metadata.branchName}, but on ${metadata.currentBranch} instead with commit ${metadata.currentBranchHash}")
          }
        }
      }
      HomeCountComposable(
        moduleCount,
        AllModulesNavRoute.AllModules
      ) { navRouteRepo.updateNavRoute(AllModulesNavRoute()) }

      HomeCountComposable(
        artifactCount,
        ArtifactsNavRoute.Artifacts
      ) { navRouteRepo.updateNavRoute(ArtifactsNavRoute()) }

      HomeCountComposable(
        ownersCount,
        OwnersNavRoute.Owners
      ) { navRouteRepo.updateNavRoute(OwnersNavRoute()) }

      HomeCountComposable(
        pluginIdsCount,
        PluginsNavRoute.Plugins
      ) { navRouteRepo.updateNavRoute(PluginsNavRoute()) }
    }
  })
}

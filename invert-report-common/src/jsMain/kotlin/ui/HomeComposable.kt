package ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.AllModulesNavRoute
import com.squareup.invert.common.navigation.routes.ArtifactsNavRoute
import com.squareup.invert.common.navigation.routes.OwnersNavRoute
import com.squareup.invert.common.navigation.routes.PluginsNavRoute
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.web.dom.*

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
                AllModulesNavRoute().navPage // TODO DONT USE DEFAULT CONSTRUCTOR
            ) { navRouteRepo.updateNavRoute(AllModulesNavRoute()) }

            HomeCountComposable(
                artifactCount,
                ArtifactsNavRoute().navPage // TODO DONT USE DEFAULT CONSTRUCTOR
            ) { navRouteRepo.updateNavRoute(ArtifactsNavRoute()) }

            HomeCountComposable(
                ownersCount,
                OwnersNavRoute.navPage
            ) { navRouteRepo.updateNavRoute(OwnersNavRoute) }

            HomeCountComposable(
                pluginIdsCount,
                PluginsNavRoute.navPage
            ) { navRouteRepo.updateNavRoute(PluginsNavRoute) }
        }
    })
}

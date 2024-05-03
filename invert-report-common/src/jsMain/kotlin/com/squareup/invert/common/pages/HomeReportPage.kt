package com.squareup.invert.common.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.DependencyGraph
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.*
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.web.dom.*
import ui.AppLink
import ui.BootstrapJumbotron
import ui.BootstrapLoadingSpinner
import kotlin.reflect.KClass

object HomeReportPage : InvertReportPage<HomeReportPage.HomeNavRoute> {
    override val navPage: NavPage = NavPage(
        pageId = "home",
        displayName = "Home",
        navIconSlug = "house",
        navRouteParser = { HomeNavRoute }
    )
    override val navRouteKClass: KClass<HomeNavRoute> = HomeNavRoute::class

    override val composableContent: @Composable (HomeNavRoute) -> Unit = { navRoute ->
        HomeComposable(navRoute)
    }

    object HomeNavRoute : BaseNavRoute(navPage)
}


@Composable
fun HomeComposable(
    homeNavRoute: HomeReportPage.HomeNavRoute,
    reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
    navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo
) {
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
                AllModulesReportPage.navPage // TODO DONT USE DEFAULT CONSTRUCTOR
            ) { navRouteRepo.updateNavRoute(AllModulesReportPage.AllModulesNavRoute()) }

            HomeCountComposable(
                artifactCount,
                ArtifactsNavRoute().navPage // TODO DONT USE DEFAULT CONSTRUCTOR
            ) { navRouteRepo.updateNavRoute(ArtifactsNavRoute()) }

            HomeCountComposable(
                ownersCount,
                OwnersReportPage.navPage
            ) { navRouteRepo.updateNavRoute(OwnersReportPage.OwnersNavRoute) }

            HomeCountComposable(
                pluginIdsCount,
                PluginsNavRoute.navPage
            ) { navRouteRepo.updateNavRoute(PluginsNavRoute) }
        }
    })
}

@Composable
fun HomeCountComposable(count: Int?, navItem: NavPage, onClick: () -> Unit) {
    P {
        count?.let {
            Text("$count")
        } ?: BootstrapLoadingSpinner()

        navItem.displayName.let { displayName ->
            AppLink({
                onClick {
                    onClick()
                }
            }) {
                Button({
                    classes("btn", "btn-link")
                }) {
                    Text(displayName)
                }
            }
        }
    }

}
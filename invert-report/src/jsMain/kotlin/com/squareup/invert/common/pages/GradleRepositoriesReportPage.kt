package com.squareup.invert.common.pages


import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.DependencyGraph
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.H4
import org.jetbrains.compose.web.dom.Li
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Ul
import ui.BootstrapLoadingSpinner
import kotlin.reflect.KClass

object GradleRepositoriesNavRoute : BaseNavRoute(GradleRepositoriesReportPage.navPage)

object GradleRepositoriesReportPage : InvertReportPage<GradleRepositoriesNavRoute> {
    override val navPage: NavPage = NavPage(
        pageId = "gradle_repos",
        displayName = "Gradle Repositories",
        navIconSlug = "database",
        navRouteParser = { GradleRepositoriesNavRoute }
    )
    override val navRouteKClass: KClass<GradleRepositoriesNavRoute> = GradleRepositoriesNavRoute::class

    override val composableContent: @Composable (GradleRepositoriesNavRoute) -> Unit = { navRoute ->
        GradleRepositoriesComposable(navRoute)
    }
}

@Composable
fun GradleRepositoriesComposable(
    navRoute: GradleRepositoriesNavRoute,
    reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
    navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo
) {
    val metadata by reportDataRepo.reportMetadata.collectAsState(null)

    if (metadata == null) {
        BootstrapLoadingSpinner()
        return
    }

    H4 {
        Text("Remote Repositories")
    }
    Br()
    Ul({ classes("fs-6") }) {
        metadata!!.mavenRepoUrls.forEach { mavenRepoUrl ->
            Li {
                A(href = mavenRepoUrl) {
                    Text(mavenRepoUrl)
                }
            }
        }
    }
}

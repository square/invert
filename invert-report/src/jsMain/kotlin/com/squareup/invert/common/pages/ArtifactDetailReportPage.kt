package com.squareup.invert.common.pages


import androidx.compose.runtime.Composable
import com.squareup.invert.common.DependencyGraph
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.ArtifactsNavRoute
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.common.pages.ArtifactDetailReportPage.navPage
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.Text
import kotlin.reflect.KClass

object ArtifactDetailReportPage : InvertReportPage<ArtifactDetailNavRoute> {
    override val navPage: NavPage = NavPage(
        pageId = "artifact_detail",
        navRouteParser = { ArtifactDetailNavRoute.parser(it) }
    )

    override val navRouteKClass: KClass<ArtifactDetailNavRoute> = ArtifactDetailNavRoute::class

    override val composableContent: @Composable (ArtifactDetailNavRoute) -> Unit = { navRoute ->
        ArtifactDetailComposable(navRoute)
    }
}

data class ArtifactDetailNavRoute(
    val group: String,
    val artifact: String,
    val version: String? = null,
) : BaseNavRoute(navPage) {
    override fun toSearchParams(): Map<String, String> = toParamsWithOnlyPageId(this)
        .also { map ->
            map[GROUP_PARAM] = group
            map[ARTIFACT_PARAM] = artifact
            version?.let {
                map[VERSION_PARAM] = it
            }
        }

    companion object {
        private const val GROUP_PARAM = "g"
        private const val ARTIFACT_PARAM = "a"
        private const val VERSION_PARAM = "v"
        fun parser(params: Map<String, String?>): NavRoute {
            val groupParam = params[GROUP_PARAM]
            val artifactParam = params[ARTIFACT_PARAM]
            return if (groupParam != null && artifactParam != null) {
                return ArtifactDetailNavRoute(
                    group = groupParam,
                    artifact = artifactParam,
                    version = params[VERSION_PARAM],
                )
            } else {
                ArtifactsNavRoute()
            }
        }
    }
}


@Composable
fun ArtifactDetailComposable(
    navRoute: ArtifactDetailNavRoute,
    reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
    navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo,
) {

    H1 { Text("Artifact Detail ${navRoute.group}:${navRoute.artifact}") }
    Br()
    H3 { Text("...") }
}

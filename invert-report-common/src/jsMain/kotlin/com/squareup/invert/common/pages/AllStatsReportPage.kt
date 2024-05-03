package com.squareup.invert.common.pages


import PagingConstants.MAX_RESULTS
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.DependencyGraph
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.common.navigation.routes.StatDetailNavRoute
import com.squareup.invert.models.CollectedStatType
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Text
import ui.*
import kotlin.reflect.KClass

object AllStatsReportPage : InvertReportPage<AllStatsReportPage.AllStatsNavRoute> {
    override val navPage: NavPage = NavPage(
        pageId = "stats",
        displayName = "Stats",
        navIconSlug = "pie-chart",
        navRouteParser = { AllStatsNavRoute.parser(it) }
    )
    override val navRouteKClass: KClass<AllStatsNavRoute> = AllStatsNavRoute::class

    override val composableContent: @Composable (AllStatsNavRoute) -> Unit = { navRoute ->
        AllStatsComposable(navRoute)
    }

    data class AllStatsNavRoute(
        val statType: CollectedStatType? = null
    ) : BaseNavRoute(
        navPage
    ) {
        override fun toSearchParams(): Map<String, String> = toParamsWithOnlyPageId(this)
            .also { params ->
                statType?.name?.let {
                    params[STAT_TYPE_PARAM] =
                        it
                }
            }

        companion object {

            private const val STAT_TYPE_PARAM = "types"

            fun parser(params: Map<String, String?>): AllStatsNavRoute {
                val statTypeParam = params[STAT_TYPE_PARAM]
                val statType = CollectedStatType.entries.firstOrNull { it.name == statTypeParam }
                return AllStatsNavRoute(
                    statType = statType,
                )
            }
        }
    }
}


@Composable
fun AllStatsComposable(
    statsNavRoute: AllStatsReportPage.AllStatsNavRoute,
    reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
    navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo
) {
    val statsDataOrig by reportDataRepo.statsData.collectAsState(null)
    val moduleToOwnerMapFlowValue by reportDataRepo.moduleToOwnerMap.collectAsState(null)

    H1 { Text("Stats") }

    if (moduleToOwnerMapFlowValue == null) {
        BootstrapLoadingSpinner()
        return
    }

    if (statsDataOrig == null) {
        BootstrapLoadingMessageWithSpinner("Loading...")
        return
    }
    val statsData = statsDataOrig!!

    val statInfos = statsData.statInfos.values

    val rows = statInfos.map { statInfo -> listOf(statInfo.name, statInfo.statType.name, statInfo.description) }

    BootstrapTable(
        headers = listOf("Name", "Type", "Description"),
        rows = rows,
        types = statInfos.map { String::class },
        maxResultsLimitConstant = MAX_RESULTS
    ) { cellValues ->
        navRouteRepo.updateNavRoute(
            StatDetailNavRoute(
                pluginIds = listOf(),
                statKeys = listOf(cellValues[0])
            )
        )
    }

    BootstrapButton("View All",
        BootstrapButtonType.PRIMARY,
        onClick = {
            navRouteRepo.updateNavRoute(
                StatDetailNavRoute(
                    pluginIds = listOf(),
                    statKeys = statInfos.map { it.name }
                )
            )
        })

}
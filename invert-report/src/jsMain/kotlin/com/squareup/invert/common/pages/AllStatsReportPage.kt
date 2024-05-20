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
import com.squareup.invert.common.pages.AllStatsReportPage.navPage
import com.squareup.invert.common.utils.FormattingUtils.formatDecimalSeparator
import com.squareup.invert.models.CollectedStatType
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Text
import ui.*
import kotlin.reflect.KClass


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

object AllStatsReportPage : InvertReportPage<AllStatsNavRoute> {
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

}

@Composable
fun AllStatsComposable(
    statsNavRoute: AllStatsNavRoute,
    reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
    navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo
) {
    val statsDataOrig by reportDataRepo.statsData.collectAsState(null)
    val statTotalsOrig by reportDataRepo.statTotals.collectAsState(null)
    val moduleToOwnerMapFlowValue by reportDataRepo.moduleToOwnerMap.collectAsState(null)

    H1 { Text("Stats") }

    if (moduleToOwnerMapFlowValue == null) {
        BootstrapLoadingSpinner()
        return
    }

    if (statsDataOrig == null || statTotalsOrig == null) {
        BootstrapLoadingMessageWithSpinner("Loading...")
        return
    }

    val statTotals = statTotalsOrig!!
    val statsData = statsDataOrig!!

    val statInfos = statsData.statInfos.values


    BootstrapRow {
        statTotals.statTotals.entries.forEach { statTotal ->
            BootstrapColumn(4) {
                BootstrapJumbotron(
                    centered = true,
                    paddingNum = 2,
                    headerContent = {
                        Text(statTotal.value.formatDecimalSeparator())
                    }
                ) {
                    A(href = "#", {
                        onClick {
                            navRouteRepo.updateNavRoute(
                                StatDetailNavRoute(
                                    pluginIds = listOf(),
                                    statKeys = listOf(statTotal.key.key)
                                )
                            )
                        }
                    }) {
                        Text(statTotal.key.description)
                    }
                }
            }
        }
    }

    BootstrapButton("View All",
        BootstrapButtonType.PRIMARY,
        onClick = {
            navRouteRepo.updateNavRoute(
                StatDetailNavRoute(
                    pluginIds = listOf(),
                    statKeys = statInfos.map { it.key }
                )
            )
        }
    )

    val stats = statsData.statInfos.values
    BootstrapClickableList("Stat", stats.map {it.key}) { clickedValue ->
        navRouteRepo.updateNavRoute(
            StatDetailNavRoute(
                statKeys = listOf(clickedValue)
            )
        )
    }

}
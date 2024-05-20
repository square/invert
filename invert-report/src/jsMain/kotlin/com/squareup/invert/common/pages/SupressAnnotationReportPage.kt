package com.squareup.invert.common.pages


import PagingConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.DependencyGraph
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.charts.ChartJsChartComposable
import com.squareup.invert.common.charts.ChartsJs
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.common.pages.SuppressAnnotationNavRoute.Companion.parser
import com.squareup.invert.models.GradlePath
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatKey
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.Text
import ui.BootstrapColumn
import ui.BootstrapLoadingMessageWithSpinner
import ui.BootstrapRow
import ui.BootstrapTable
import kotlin.reflect.KClass

data class SuppressAnnotationNavRoute(
    val module: String? = null,
    val configuration: String? = null,
) : BaseNavRoute(SuppressAnnotationReportPage.navPage) {

    override fun toSearchParams(): Map<String, String> = toParamsWithOnlyPageId(this)
        .also { params ->
            module?.let {
                params[MODULE_PARAM] = module
            }
            configuration?.let {
                params[CONFIGURATION_PARAM] = configuration
            }
        }

    companion object {

        private const val MODULE_PARAM = "module"
        private const val CONFIGURATION_PARAM = "configuration"

        fun parser(params: Map<String, String?>): SuppressAnnotationNavRoute {
            return SuppressAnnotationNavRoute(
                module = params[MODULE_PARAM],
                configuration = params[CONFIGURATION_PARAM]
            )
        }
    }
}


object SuppressAnnotationReportPage : InvertReportPage<SuppressAnnotationNavRoute> {
    override val navPage: NavPage = NavPage(
        pageId = "suppress_annotation",
        displayName = "Suppress Annotation",
        navIconSlug = "at",
        navRouteParser = { parser(it) }
    )

    override val navRouteKClass: KClass<SuppressAnnotationNavRoute> = SuppressAnnotationNavRoute::class

    override val composableContent: @Composable (SuppressAnnotationNavRoute) -> Unit = { navRoute ->
        SuppressAnnotationComposable(navRoute)
    }
}

data class SuppressTypeByModule(
    val gradlePath: GradlePath,
    val suppressType: String,
    val count: Int,
    val statKey: StatKey,
)

@Composable
fun SuppressAnnotationComposable(
    dependencyGraphNavRoute: SuppressAnnotationNavRoute,
    reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
    navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo,
) {
    val allModules by reportDataRepo.allModules.collectAsState(null)
    val statsData by reportDataRepo.statsData.collectAsState(null)
    val statInfos by reportDataRepo.statInfos.collectAsState(null)
    val statsByModule = statsData?.statsByModule

    if (allModules == null || statInfos == null) {
        BootstrapLoadingMessageWithSpinner()
        return
    }

    val listOfSuppressTypeByModule = mutableListOf<SuppressTypeByModule>().apply {
        statsByModule?.keys?.map { modulePath ->
            statsByModule[modulePath]?.forEach { (statKey, stat) ->
                val statMetadataForStatKey = statsData?.statInfos?.get(statKey)
                if ((stat is Stat.NumericStat) && statMetadataForStatKey?.category == "suppress_annotation") {
                    this.add(
                        SuppressTypeByModule(
                            gradlePath = modulePath,
                            suppressType = statMetadataForStatKey.description,
                            count = stat.value,
                            statKey = statMetadataForStatKey.key
                        )
                    )
                }
            }
        }
    }

    val distinctSuppressTypes: List<SuppressTypeByModule> = listOfSuppressTypeByModule.distinctBy { it.suppressType }
    val suppressTypeToTotalCount = distinctSuppressTypes
        .map { suppressTypeByModule: SuppressTypeByModule ->
            suppressTypeByModule to listOfSuppressTypeByModule
                .filter { it.suppressType == suppressTypeByModule.suppressType }
                .sumOf { it.count }
        }
        .sortedByDescending { it.second }

    val gradlePathToTotalCount: Map<GradlePath, Int> = listOfSuppressTypeByModule
        .groupBy { it.gradlePath }
        .map { (gradlePath, suppressTypeByModule) -> gradlePath to suppressTypeByModule.sumOf { it.count } }
        .toMap()


    val labels = mutableListOf<String>()
    val chartData = statsByModule?.keys?.map { modulePath ->
        labels.add(modulePath)
        var count = 0
        val onlySuppressStatsForModule: List<Stat.NumericStat>? =
            statsByModule[modulePath]?.filter { (statKey, stat) ->
                ((stat is Stat.NumericStat) && statKey.startsWith("suppress_annotation_"))
            }?.map { it.value as Stat.NumericStat }

        println("onlySuppressStatsForModule")
        println(onlySuppressStatsForModule)

        onlySuppressStatsForModule?.forEach {
            count += it.value
        }

        statsByModule[modulePath]?.forEach { entry ->
            val statKey = entry.key
            val stat = entry.value
            if ((stat is Stat.NumericStat) && statKey.startsWith("suppress_annotation_")) {
                count += stat.value
            }
        }
        count
    } ?: emptyList()

    BootstrapRow {
        BootstrapColumn(12) {
            H3 {
                Text("Top Suppressions by Total Count")
            }
            BootstrapRow {
                BootstrapColumn(6) {
                    BootstrapTable(
                        headers = listOf("@Suppress Type", "Count"),
                        rows = suppressTypeToTotalCount.map {
                            listOf(it.first.suppressType, it.second.toString())
                        },
                        types = listOf(String::class, Int::class),
                        maxResultsLimitConstant = PagingConstants.MAX_RESULTS,
                        onItemClickCallback = { row ->
                            navRouteRepo.updateNavRoute(
                                StatDetailNavRoute(
                                    statKeys = listOf(statInfos!!.firstOrNull { it.description == row[0] }).mapNotNull { it?.key }
                                )
                            )
                        },
                        sortByColumn = 1,
                        sortAscending = false,
                    )
                }
                BootstrapColumn(6) {
                    ChartJsChartComposable(
                        domId = "chart-js-pie-graph1",
                        type = "pie",
                        data = ChartsJs.ChartJsData(
                            labels = suppressTypeToTotalCount.map { it.first.suppressType },
                            datasets = listOf(
                                ChartsJs.ChartJsDataset(
                                    label = "Number of Suppressions",
                                    data = suppressTypeToTotalCount.map { it.second },
                                    borderWidth = 1
                                )
                            ),
                        ),
                        onClick = { label: String, value: Int ->
                            navRouteRepo.updateNavRoute(
                                StatDetailNavRoute(
                                    statKeys = listOf(
                                        statsData?.statInfos?.values?.firstOrNull { it.description == label }?.key ?: ""
                                    ),
                                )
                            )
                        }
                    )
                }
            }

            H3 {
                Text("Modules with Most Suppressions")
            }
            BootstrapRow {
                BootstrapColumn(6) {
                    BootstrapTable(
                        headers = listOf("Module", "Total @Suppress Count"),
                        rows = gradlePathToTotalCount.map {
                            listOf(it.key, it.value.toString())
                        },
                        types = listOf(String::class, Int::class),
                        maxResultsLimitConstant = PagingConstants.MAX_RESULTS,
                        onItemClickCallback = { row ->
                            navRouteRepo.updateNavRoute(
                                ModuleDetailNavRoute(
                                    path = row[0]
                                )
                            )
                        },
                        sortByColumn = 1,
                        sortAscending = false,
                    )
                }
                BootstrapColumn(6) {
                    ChartJsChartComposable(
                        domId = "chart-js-pie-graph",
                        type = "bar",
                        data = ChartsJs.ChartJsData(
                            labels = labels,
                            datasets = listOf(
                                ChartsJs.ChartJsDataset(
                                    label = "Number of Suppressions",
                                    data = chartData,
                                    borderWidth = 1
                                )
                            ),
                        ),
                        onClick = { label: String, value: Int ->
                            navRouteRepo.updateNavRoute(
                                ModuleDetailNavRoute(
                                    path = label
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}
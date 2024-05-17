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
import com.squareup.invert.common.pages.SuppressAnnotationGraphNavRoute.Companion.parser
import com.squareup.invert.models.InvertSerialization
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Canvas
import renderChartJs
import ui.BootstrapColumn
import ui.BootstrapLoadingMessageWithSpinner
import ui.BootstrapRow
import kotlin.reflect.KClass

data class SuppressAnnotationGraphNavRoute(
    val module: String? = null,
    val configuration: String? = null,
) : BaseNavRoute(SuppressAnnotationGraphReportPage.navPage) {

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

        fun parser(params: Map<String, String?>): SuppressAnnotationGraphNavRoute {
            return SuppressAnnotationGraphNavRoute(
                module = params[MODULE_PARAM],
                configuration = params[CONFIGURATION_PARAM]
            )
        }
    }
}


object SuppressAnnotationGraphReportPage : InvertReportPage<SuppressAnnotationGraphNavRoute> {
    override val navPage: NavPage = NavPage(
        pageId = "suppress_annotation",
        displayName = "Suppress Annotation",
        navIconSlug = "diagram-3",
        navRouteParser = { parser(it) }
    )

    override val navRouteKClass: KClass<SuppressAnnotationGraphNavRoute> = SuppressAnnotationGraphNavRoute::class

    override val composableContent: @Composable (SuppressAnnotationGraphNavRoute) -> Unit = { navRoute ->
        SuppressAnnotationGraphComposable(navRoute)
    }
}


@Serializable
data class ScaleInfo(
    val beginAtZero: Boolean = true,
)

@Serializable
data class ChartJsScales(
    val y: ScaleInfo = ScaleInfo(),
)

@Serializable
data class ChartJsOptions(
    val scales: ChartJsScales,
)

@Serializable
data class ChartJsDataset(
    val label: String,
    val data: List<Int>,
    val borderWidth: Int = 1,
)

@Serializable
data class ChartJsData(
    val labels: List<String>,
    val datasets: List<ChartJsDataset>,
)

@Serializable
data class ChartJsParam(
    val type: String,
    val data: ChartJsData,
    val options: ChartJsOptions,
)

@Composable
fun SuppressAnnotationGraphComposable(
    dependencyGraphNavRoute: SuppressAnnotationGraphNavRoute,
    reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
    navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo,
) {
    val rootModulePath = dependencyGraphNavRoute.module ?: ":api:sales-report:impl"
    val allModules by reportDataRepo.allModules.collectAsState(null)

    if (allModules == null) {
        BootstrapLoadingMessageWithSpinner()
        return
    }


    val pieChartData = ChartJsParam(
        type = "doughnut",
        data = ChartJsData(
            labels = listOf("Red", "Blue", "Yellow", "Green"),
            datasets = listOf(
                ChartJsDataset(
                    label = "Number of Votes",
                    data = listOf(12, 19, 3, 5),
                    borderWidth = 1
                )
            ),
        ),
        options = ChartJsOptions(
            scales = ChartJsScales(
                y = ScaleInfo(
                    beginAtZero = true
                )
            )
        )
    )

    BootstrapRow {
        BootstrapColumn(6) {
            ChartJsComposable("chart-js-pie-graph", pieChartData)
        }
        BootstrapColumn(6) {
            ChartJsComposable("chart-js-bar-graph", pieChartData.copy(type = "bar"))
        }
    }


}

@Composable
fun ChartJsComposable(graphDomId: String, pieChartData: ChartJsParam) {
    // Good to go
    Canvas({
        id(graphDomId)
        style {
            border {
                width(2.px)
                style(LineStyle.Solid)
                color = Color.black
            }
            width(100.percent)
            height(400.px)
        }
    })
    CoroutineScope(Dispatchers.Main).launch {
        delay(500)
        renderChartJs(
            graphDomId,
            InvertSerialization.InvertJson.encodeToString(
                ChartJsParam.serializer(), pieChartData
            ),
        )
    }
}


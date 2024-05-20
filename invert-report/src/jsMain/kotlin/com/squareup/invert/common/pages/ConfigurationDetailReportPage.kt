package com.squareup.invert.common.pages


import PagingConstants.MAX_RESULTS
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.CollectedDataRepo
import com.squareup.invert.common.DependencyGraph
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.models.ConfigurationName
import com.squareup.invert.models.GradlePath
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.mapLatest
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Text
import ui.BootstrapLoadingMessageWithSpinner
import ui.BootstrapTable
import kotlin.reflect.KClass

data class ConfigurationDetailNavRoute(
    val configurationName: ConfigurationName,
) : BaseNavRoute(ConfigurationDetailReportPage.navPage) {
    override fun toSearchParams() = toParamsWithOnlyPageId(this)
        .also { map ->
            map[CONFIGURATION_PARAM] = configurationName
        }

    companion object {

        private const val CONFIGURATION_PARAM = "configuration"
        fun parser(params: Map<String, String?>): NavRoute {
            val configurationName = params[CONFIGURATION_PARAM]
            return if (!configurationName.isNullOrEmpty()) {
                ConfigurationDetailNavRoute(
                    configurationName = configurationName
                )
            } else {
                ConfigurationsNavRoute
            }
        }
    }
}

object ConfigurationDetailReportPage : InvertReportPage<ConfigurationDetailNavRoute> {
    override val navPage: NavPage = NavPage(
        pageId = "configuration_detail",
        navRouteParser = { ConfigurationDetailNavRoute.parser(it) }
    )
    override val navRouteKClass: KClass<ConfigurationDetailNavRoute> = ConfigurationDetailNavRoute::class

    override val composableContent: @Composable (ConfigurationDetailNavRoute) -> Unit = { navRoute ->
        ConfigurationDetailComposable(navRoute)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun ConfigurationDetailComposable(
    navRoute: ConfigurationDetailNavRoute,
    reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
    collectedDataRepo: CollectedDataRepo = DependencyGraph.collectedDataRepo,
    navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo,
) {
    val configurationName = navRoute.configurationName

    val modulesUsingConfiguration by collectedDataRepo.configurations.mapLatest {

        val list = mutableListOf<GradlePath>()

        it?.moduleToAllConfigurationNames?.entries?.forEach { (moduleName, allConfigurationNames) ->
            if (allConfigurationNames.contains(configurationName)) {
                list.add(moduleName)
            }
        }
        list
    }.collectAsState(null)

    if (modulesUsingConfiguration == null) {
        H1 {
            BootstrapLoadingMessageWithSpinner("Loading Configuration Usage...")
        }
        return
    }
    H1 {
        Text("Modules with Configuration: $configurationName")
    }
    val configurationUsages = modulesUsingConfiguration ?: emptyList()
    BootstrapTable(
        headers = listOf("Module"),
        rows = configurationUsages.map { listOf(it) },
        types = listOf(String::class),
        maxResultsLimitConstant = MAX_RESULTS,
        onItemClickCallback = {
            navRouteRepo.updateNavRoute(
                ModuleDetailNavRoute(
                    it[0]
                )
            )
        }
    )
}
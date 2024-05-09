package com.squareup.invert.common.pages


import PagingConstants.MAX_RESULTS
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.DependencyGraph
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import kotlinx.browser.window
import ui.*
import kotlin.reflect.KClass

object ConfigurationsNavRoute : BaseNavRoute(NavPage(
    pageId = "configurations",
    displayName = "Configurations",
    navIconSlug = "gear",
    navRouteParser = { ConfigurationsNavRoute }
))

object ConfigurationsReportPage : InvertReportPage<ConfigurationsNavRoute> {
    override val navPage: NavPage = NavPage(
        pageId = "blank",
        navRouteParser = { OwnersNavRoute }
    )
    override val navRouteKClass: KClass<ConfigurationsNavRoute> = ConfigurationsNavRoute::class

    override val composableContent: @Composable (ConfigurationsNavRoute) -> Unit = { navRoute ->
        ConfigurationsComposable(navRoute)
    }

    object OwnersNavRoute : BaseNavRoute(navPage)
}


@Composable
fun ConfigurationsComposable(
    navRoute: ConfigurationsNavRoute,
    reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo
) {
    val allAvailableConfigurationNamesOrig by reportDataRepo.allAvailableConfigurationNames.collectAsState(null)
    val allAvailableConfigurationNames = allAvailableConfigurationNamesOrig
    val listOrig by reportDataRepo.allAnalyzedConfigurationNames.collectAsState(null)
    val list = listOrig
    if (list == null || allAvailableConfigurationNames == null) {
        BootstrapLoadingMessageWithSpinner()
        return
    }

    val count = list.size
    TitleRow("Analyzed Gradle Configurations ($count of ${allAvailableConfigurationNames.size} Total)")


    BootstrapRow {
        BootstrapColumn(12) {
            BootstrapClickableList("Analyzed Configurations", list, MAX_RESULTS) { item ->
                window.alert("Clicked $item")
            }
        }
    }

    BootstrapTable(
        headers = listOf("Other (Not Analyzed) Configurations"),
        rows = mutableListOf<List<String>>().also { rows ->
            allAvailableConfigurationNames.forEach { availableConfigurationName ->
                val wasScanned = list.contains(availableConfigurationName)
                if (!wasScanned) {
                    rows.add(
                        mutableListOf(
                            availableConfigurationName,
                        )
                    )
                }
            }
        },
        types = listOf<KClass<*>>(String::class),
        maxResultsLimitConstant = MAX_RESULTS,
        onItemClick = {
            window.alert("Clicked ${it[0]}")
        }
    )
}
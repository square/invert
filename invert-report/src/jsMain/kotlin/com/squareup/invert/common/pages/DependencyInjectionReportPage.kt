package com.squareup.invert.common.pages


import PagingConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.DependencyGraph
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.common.pages.DependencyInjectionNavRoute.Companion.parser
import com.squareup.invert.models.GradlePluginId
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatKey
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.Text
import ui.*
import kotlin.reflect.KClass

data class DependencyInjectionNavRoute(
    val pluginIds: List<GradlePluginId>,
    val statKeys: List<String>,
    val moduleQuery: String? = null
) : BaseNavRoute(DependencyInjectionReportPage.navPage) {

    override fun toSearchParams(): Map<String, String> = toParamsWithOnlyPageId(this)
        .also { params ->
            params[PLUGIN_IDS_PARAM] = pluginIds.joinToString(separator = ",")
            params[STATKEYS_PARAM] = statKeys.joinToString(separator = ",")
            moduleQuery?.let {
                params[MODULE_QUERY_PARAM] = it
            }
        }

    companion object {

        private const val PLUGIN_IDS_PARAM = "plugins"
        private const val STATKEYS_PARAM = "statkeys"
        private const val MODULE_QUERY_PARAM = "modulequery"

        fun parser(params: Map<String, String?>): DependencyInjectionNavRoute {
            val pluginIds = params[PLUGIN_IDS_PARAM]?.split(",")?.filter { it.isNotBlank() } ?: listOf()
            val statKeys = params[STATKEYS_PARAM]?.split(",")?.filter { it.isNotBlank() } ?: listOf()
            val moduleQuery = params[MODULE_QUERY_PARAM]
            return DependencyInjectionNavRoute(
                pluginIds = pluginIds,
                statKeys = statKeys,
                moduleQuery = moduleQuery
            )
        }
    }
}

object DependencyInjectionReportPage : InvertReportPage<DependencyInjectionNavRoute> {
    override val navPage: NavPage = NavPage(
        pageId = "dependency_injection",
        displayName = "Dependency Injection",
        navRouteParser = { parser(it) }
    )

    override val navRouteKClass: KClass<DependencyInjectionNavRoute> = DependencyInjectionNavRoute::class

    override val composableContent: @Composable (DependencyInjectionNavRoute) -> Unit = { navRoute ->
        DependencyInjectionComposable(navRoute)
    }
}

@Composable
fun DependencyInjectionComposable(
    statsNavRoute: DependencyInjectionNavRoute,
    reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
    navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo,
) {
    val allPluginIds by reportDataRepo.allPluginIds.collectAsState(null)
    val statsData by reportDataRepo.statsData.collectAsState(null)
    val allModulesOrig by reportDataRepo.allModules.collectAsState(null)
    val moduleToOwnerMapFlowValue by reportDataRepo.moduleToOwnerMap.collectAsState(null)


    H1 { Text("Dependency Injection") }

    if (moduleToOwnerMapFlowValue == null) {
        BootstrapLoadingSpinner()
        return
    }

    val query = statsNavRoute.moduleQuery



    if (allModulesOrig == null) {
        return
    }
    val allModules1 = allModulesOrig!!

    val allModules = if (query != null && query != ":" && query.isNotEmpty()) {
        allModules1.filter { it.contains(query) }
    } else {
        allModules1
    }

    BootstrapSearchBox(
        query = query ?: "",
        placeholderText = "Module Query...",
    ) {
        navRouteRepo.updateNavRoute(statsNavRoute.copy(moduleQuery = it))
    }


    val STAT_KEY = "DiProvidesAndInjects"

    val statsColumns = mutableListOf<List<String>>().apply {
        val diData = statsData?.statInfos?.get(STAT_KEY)
        add(
            allModules.map { gradlePath ->
                val statsDataForModule: Map<StatKey, Stat>? = statsData?.statsByModule?.get(gradlePath)
                val stat = statsDataForModule?.get(STAT_KEY)
                when (stat) {
                    is Stat.ProvidesAndInjectsStat -> {
                        stat.value.toString().replace(",", "\n")
                    }

                    else -> ""
                }
            }
        )
    }

    val headers = mutableListOf("Module")
        .apply {
            add(STAT_KEY)
        }
    val values: List<List<String>> = allModules.mapIndexed { idx, modulePath ->
        mutableListOf(
            allModules[idx]
        ).apply {
            statsColumns.forEach {
                add(it[idx])
            }
        }
    }.filter {
        var hasValue = false
        it.forEachIndexed { idx, str ->
            if (idx > 0 && str.isNotEmpty()) {
                hasValue = true
            }
        }
        hasValue
    }

    if (statsData == null) {
        BootstrapLoadingMessageWithSpinner("Loading...")
    } else {
        if (values.isNotEmpty()) {
            BootstrapTable(
                headers = headers,
                rows = values,
                types = headers.map { String::class },
                maxResultsLimitConstant = PagingConstants.MAX_RESULTS
            ) { cellValues ->
                navRouteRepo.updateNavRoute(ModuleDetailNavRoute(cellValues[0]))
            }
        } else {
            H3 { Text("No Collected Stats of Type(s) ${statsNavRoute.statKeys}") }
        }
    }
}
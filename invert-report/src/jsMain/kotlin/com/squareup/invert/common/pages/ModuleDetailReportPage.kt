package com.squareup.invert.common.pages


import PagingConstants.MAX_RESULTS
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.DependencyGraph
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.common.pages.ModuleDetailNavRoute.Companion.parser
import com.squareup.invert.models.ConfigurationName
import com.squareup.invert.models.GradlePath
import com.squareup.invert.models.OwnerName
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Text
import ui.*
import kotlin.reflect.KClass

data class ModuleDetailNavRoute(
    val path: GradlePath,
    val configurationName: ConfigurationName? = null,
) : BaseNavRoute(ModuleDetailReportPage.navPage) {
    override fun toSearchParams() = toParamsWithOnlyPageId(this)
        .also { map ->
            map[PATH_PARAM] = path
            configurationName?.let {
                map[CONFIGURATION_PARAM] = it
            }
        }

    companion object {

        private const val PATH_PARAM = "path"
        private const val CONFIGURATION_PARAM = "configuration"
        fun parser(params: Map<String, String?>): NavRoute {
            val path = params[PATH_PARAM]
            val configurationName = params[CONFIGURATION_PARAM]
            return if (!path.isNullOrEmpty()) {
                ModuleDetailNavRoute(
                    path = path,
                    configurationName = configurationName
                )
            } else {
                AllModulesNavRoute()
            }
        }
    }
}


object ModuleDetailReportPage : InvertReportPage<ModuleDetailNavRoute> {
    override val navPage: NavPage = NavPage(
        pageId = "module_detail",
        navRouteParser = { parser(it) }
    )
    override val navRouteKClass: KClass<ModuleDetailNavRoute> = ModuleDetailNavRoute::class

    override val composableContent: @Composable (ModuleDetailNavRoute) -> Unit = { navRoute ->
        ModuleDetailComposable(navRoute)
    }
}

@Composable
fun ModuleDetailComposable(
    navRoute: ModuleDetailNavRoute,
    reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
    navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo,
) {
    val modulePath = navRoute.path

    val directDependenciesMapOrig by reportDataRepo.directDependenciesOf(modulePath).collectAsState(null)
    val moduleToOwnerMapCollected by reportDataRepo.moduleToOwnerMap.collectAsState(null)

    if (moduleToOwnerMapCollected == null) {
        H1 {
            BootstrapLoadingMessageWithSpinner("Loading Module Ownership...")
        }
        return
    }
    val moduleToOwnerMap = moduleToOwnerMapCollected!!
    val ownerName: OwnerName? = moduleToOwnerMap[modulePath]
    ownerName?.let {
        H1 {
            Text("Module $modulePath is owned by ")
            AppLink({
                onClick {
                    navRouteRepo.updateNavRoute(OwnerDetailNavRoute(ownerName))
                }
            }) {
                Text(ownerName)
            }
        }
    }
    val tabs = mutableListOf<BootstrapTabData>()

    if (directDependenciesMapOrig == null) {
        BootstrapLoadingMessageWithSpinner("Loading Direct Dependencies...")
        return
    }
    directDependenciesMapOrig?.keys?.forEach { configurationName ->
        tabs.add(
            BootstrapTabData("Direct Dependencies for $configurationName") {
                val rows = directDependenciesMapOrig?.get(configurationName)
                    ?.filter { it.startsWith(":") }
                    ?.sorted() ?: listOf()
                BootstrapTable(
                    headers = listOf("Module"),
                    rows = rows.map { listOf(it) },
                    types = listOf(String::class),
                    maxResultsLimitConstant = MAX_RESULTS,
                    onItemClick = {
                        navRouteRepo.updateNavRoute(ModuleDetailNavRoute(it[0]))
                    }
                )
            }
        )
    }


    val configurationToDependencyMapCollected by reportDataRepo.dependenciesOf(navRoute.path).collectAsState(null)
    if (configurationToDependencyMapCollected == null) {
        H1 {
            BootstrapLoadingMessageWithSpinner("Loading Transitive Dependencies...")
        }
        return
    }

    val configurationToDependencyMap = configurationToDependencyMapCollected!!
    configurationToDependencyMap.keys.forEach { configurationName ->
        tabs.add(
            BootstrapTabData("Transitive Dependencies for $configurationName") {
                val rows = configurationToDependencyMap[configurationName]
                    ?.filter { it.startsWith(":") }
                    ?.sorted() ?: listOf()
                BootstrapTable(
                    headers = listOf("Module"),
                    rows = rows.map { listOf(it) },
                    types = listOf(String::class),
                    maxResultsLimitConstant = MAX_RESULTS,
                    onItemClick = {
                        navRouteRepo.updateNavRoute(ModuleDetailNavRoute(it[0]))
                    }
                )
            }
        )
    }
    tabs.add(BootstrapTabData("Module Used By...") {
        val moduleUsageCollected by reportDataRepo.moduleUsedBy(modulePath).collectAsState(null)
        if (moduleUsageCollected == null) {
            H1 {
                BootstrapLoadingMessageWithSpinner("Loading Module Usage...")
            }
            return@BootstrapTabData
        }
        val moduleUsage = moduleUsageCollected!!
        BootstrapTable(
            headers = listOf("Module", "Used in Configurations"),
            rows = moduleUsage.keys.map { key -> listOf(key, moduleUsage[key].toString()) },
            types = moduleUsage.keys.map { String::class },
            maxResultsLimitConstant = MAX_RESULTS,
            onItemClick = {
                navRouteRepo.updateNavRoute(
                    ModuleDetailNavRoute(
                        it[0]
                    )
                )
            }
        )
    })
    BootstrapTabPane(tabs)
}
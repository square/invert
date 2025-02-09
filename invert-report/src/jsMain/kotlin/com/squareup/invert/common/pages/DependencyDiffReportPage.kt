package com.squareup.invert.common.pages


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
import com.squareup.invert.common.pages.DependencyDiffNavRoute.Companion.parser
import com.squareup.invert.models.ConfigurationName
import com.squareup.invert.models.DependencyId
import com.squareup.invert.models.ModulePath
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Datalist
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import ui.BootstrapButton
import ui.BootstrapColumn
import ui.BootstrapRow
import ui.BootstrapSearchBox
import ui.BootstrapSettingsCheckbox
import kotlin.reflect.KClass


data class DependencyDiffNavRoute(
    val moduleA: ModulePath? = null,
    val moduleB: ModulePath? = null,
    val configurationA: ConfigurationName? = null,
    val configurationB: ConfigurationName? = null,
    val includeArtifacts: Boolean = false,
    val showMatching: Boolean = false,
) : BaseNavRoute(DependencyDiffReportPage.navPage) {

    override fun toSearchParams() = toParamsWithOnlyPageId(this).also { map ->
        moduleA?.let {
            map[MODULE_A_PARAM] = it
        }
        moduleB?.let {
            map[MODULE_B_PARAM] = it
        }
        configurationA?.let {
            map[CONFIGURATION_A_PARAM] = it
        }
        configurationB?.let {
            map[CONFIGURATION_B_PARAM] = it
        }
        map[INCLUDE_ARTIFACTS_PARAM] = includeArtifacts.toString()
        map[SHOW_MATCHING_PARAM] = showMatching.toString()
    }

    companion object {

        private const val MODULE_A_PARAM = "module_a"
        private const val MODULE_B_PARAM = "module_b"
        private const val CONFIGURATION_A_PARAM = "configuration_a"
        private const val CONFIGURATION_B_PARAM = "configuration_b"
        private const val INCLUDE_ARTIFACTS_PARAM = "include_artifacts"
        private const val SHOW_MATCHING_PARAM = "show_matching"

        fun parser(params: Map<String, String?>): NavRoute {
            val moduleA = params[MODULE_A_PARAM]
            val moduleB = params[MODULE_B_PARAM]
            val configurationA = params[CONFIGURATION_A_PARAM]
            val configurationB = params[CONFIGURATION_B_PARAM]
            val includeArtifacts = params[INCLUDE_ARTIFACTS_PARAM].toBoolean()
            val showMatching = params[SHOW_MATCHING_PARAM].toBoolean()
            return DependencyDiffNavRoute(
                moduleA = moduleA,
                moduleB = moduleB,
                configurationA = configurationA,
                configurationB = configurationB,
                includeArtifacts = includeArtifacts,
                showMatching = showMatching,
            )
        }
    }
}


object DependencyDiffReportPage : InvertReportPage<DependencyDiffNavRoute> {
    override val navPage: NavPage = NavPage(
        pageId = "dependency_diff",
        displayName = "Dependency Diff",
        navIconSlug = "folder",
        navRouteParser = { parser(it) }
    )

    override val navRouteKClass: KClass<DependencyDiffNavRoute> = DependencyDiffNavRoute::class

    override val composableContent: @Composable (DependencyDiffNavRoute) -> Unit = { navRoute ->
        ModuleDependencyDiffComposable(navRoute)
    }
}


@Composable
fun ModuleDependencyDiffComposable(
    navRoute: DependencyDiffNavRoute,
    reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
    navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo,
) {
    H1 { Text("Module Dependency Diff between ${navRoute.moduleA ?: "..."} and ${navRoute.moduleB ?: "..."}") }

    val allModules by reportDataRepo.allModules.collectAsState(null)

    val moduleADeps by reportDataRepo.dependenciesOf(navRoute.moduleA).collectAsState(null)
    val moduleBDeps by reportDataRepo.dependenciesOf(navRoute.moduleB).collectAsState(null)

    val aDepsForConfiguration = moduleADeps?.get(navRoute.configurationA)
    val bDepsForConfiguration = moduleBDeps?.get(navRoute.configurationB)

    val ALL_MODULES_DATALIST_ID = "all_modules_datalist"
    val MODULE_A_CONFIGURATIONS_DATALIST = "module_a_configurations_datalist"
    val MODULE_B_CONFIGURATIONS_DATALIST = "module_b_configurations_datalist"

    Datalist({ id(ALL_MODULES_DATALIST_ID) }) { allModules?.sorted()?.forEach { Option(it) } }
    Datalist({ id(MODULE_A_CONFIGURATIONS_DATALIST) }) { moduleADeps?.keys?.sorted()?.forEach { Option(it) } }
    Datalist({ id(MODULE_B_CONFIGURATIONS_DATALIST) }) { moduleBDeps?.keys?.sorted()?.forEach { Option(it) } }


    val superSetOfDeps = mutableSetOf<DependencyId>().apply {
        aDepsForConfiguration?.let { addAll(it) }
        bDepsForConfiguration?.let { addAll(it) }
    }

    BootstrapRow {
        BootstrapColumn(5) {
            BootstrapSearchBox(
                query = navRoute.moduleA ?: "",
                placeholderText = "Module A...",
                dataListId = ALL_MODULES_DATALIST_ID,
            ) {
                navRouteRepo.replaceNavRoute(navRoute.copy(moduleA = it))
            }
            BootstrapSearchBox(
                query = navRoute.configurationA ?: "",
                placeholderText = "Configuration A...",
                dataListId = MODULE_A_CONFIGURATIONS_DATALIST,
            ) {
                navRouteRepo.replaceNavRoute(navRoute.copy(configurationA = it))
            }
        }
        BootstrapColumn(2) {
            BootstrapButton("↔️") {
                navRouteRepo.replaceNavRoute(
                    navRoute.copy(
                        moduleA = navRoute.moduleB,
                        moduleB = navRoute.moduleA,
                        configurationA = navRoute.configurationB,
                        configurationB = navRoute.configurationA
                    )
                )
            }
        }
        BootstrapColumn(5) {
            BootstrapSearchBox(
                query = navRoute.moduleB ?: "",
                placeholderText = "Module B...",
                dataListId = ALL_MODULES_DATALIST_ID,
            ) {
                navRouteRepo.replaceNavRoute(navRoute.copy(moduleB = it))
            }
            BootstrapSearchBox(
                query = navRoute.configurationB ?: "",
                placeholderText = "Configuration B...",
                dataListId = MODULE_A_CONFIGURATIONS_DATALIST,
            ) {
                navRouteRepo.replaceNavRoute(navRoute.copy(configurationB = it))
            }
        }
    }

    BootstrapSettingsCheckbox("Include Artifacts", navRoute.includeArtifacts) {
        navRouteRepo.replaceNavRoute(navRoute.copy(includeArtifacts = it))
    }
    BootstrapSettingsCheckbox("Show Matching", navRoute.showMatching) {
        navRouteRepo.replaceNavRoute(navRoute.copy(showMatching = it))
    }

    Br()
    if (navRoute.moduleA.isNullOrBlank()) {
        H3({ classes("text-danger") }) { Text("Please Select Module A") }
    } else if (navRoute.moduleB.isNullOrBlank()) {
        H3({ classes("text-danger") }) { Text("Please Select Module B") }
    } else if (navRoute.configurationA.isNullOrBlank()) {
        H3({ classes("text-danger") }) { Text("Please Select Configuration for Module A") }
    } else if (navRoute.configurationB.isNullOrBlank()) {
        H3({ classes("text-danger") }) { Text("Please Select Configuration for Module B") }
    }
    Br()

    P({
        classes("fw-semibold", "lh-1", "font-monospace")
    }) {
        superSetOfDeps
            .sorted()
            .filter { dependencyId ->
                if (!navRoute.includeArtifacts) {
                    dependencyId.startsWith(":")
                } else {
                    true
                }
            }
            .forEach { dependencyId ->
                val inA = aDepsForConfiguration?.contains(dependencyId) ?: false
                val inB = bDepsForConfiguration?.contains(dependencyId) ?: false
                val inBoth = inA && inB

                if (!inBoth) {
                    if (inA) {
                        Span({ classes("text-danger") }) {
                            Text("- $dependencyId")
                        }
                    } else {
                        Span({ classes("text-success") }) {
                            Text("+ $dependencyId")
                        }
                    }
                    Br()
                } else if (navRoute.showMatching) {
                    Span({
                        classes("fw-normal")
                    }) {
                        Text("* $dependencyId")
                    }
                    Br()
                }
            }
    }
}





package ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.models.DependencyId
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.routes.DependencyDiffNavRoute
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Datalist
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text


@Composable
fun ModuleDependencyDiffComposable(
    reportDataRepo: ReportDataRepo,
    navRouteRepo: NavRouteRepo,
    navRoute: DependencyDiffNavRoute
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
                navRouteRepo.updateNavRoute(navRoute.copy(moduleA = it))
            }
            BootstrapSearchBox(
                query = navRoute.configurationA ?: "",
                placeholderText = "Configuration A...",
                dataListId = MODULE_A_CONFIGURATIONS_DATALIST,
            ) {
                navRouteRepo.updateNavRoute(navRoute.copy(configurationA = it))
            }
        }
        BootstrapColumn(2) {
            BootstrapButton("↔️") {
                navRouteRepo.updateNavRoute(
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
                navRouteRepo.updateNavRoute(navRoute.copy(moduleB = it))
            }
            BootstrapSearchBox(
                query = navRoute.configurationB ?: "",
                placeholderText = "Configuration B...",
                dataListId = MODULE_A_CONFIGURATIONS_DATALIST,
            ) {
                navRouteRepo.updateNavRoute(navRoute.copy(configurationB = it))
            }
        }
    }

    BootstrapSettingsCheckbox("Include Artifacts", navRoute.includeArtifacts) {
        navRouteRepo.updateNavRoute(navRoute.copy(includeArtifacts = it))
    }
    BootstrapSettingsCheckbox("Show Matching", navRoute.showMatching) {
        navRouteRepo.updateNavRoute(navRoute.copy(showMatching = it))
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



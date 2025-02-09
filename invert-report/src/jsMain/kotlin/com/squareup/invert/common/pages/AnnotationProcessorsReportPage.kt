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
import com.squareup.invert.models.ConfigurationName
import com.squareup.invert.models.DependencyId
import com.squareup.invert.models.ModulePath
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.H4
import org.jetbrains.compose.web.dom.Text
import ui.BoostrapExpandingCard
import ui.BootstrapAccordion
import ui.BootstrapLoadingSpinner
import ui.BootstrapTable
import kotlin.reflect.KClass

object AnnotationProcessorsReportPage : InvertReportPage<AnnotationProcessorsReportPage.AnnotationProcessorsNavRoute> {
    override val navPage: NavPage = NavPage(
        pageId = "annotation_processors",
        displayName = "Annotation Processors",
        navIconSlug = "cpu",
        navRouteParser = { AnnotationProcessorsNavRoute }
    )
    override val navRouteKClass: KClass<AnnotationProcessorsNavRoute> = AnnotationProcessorsNavRoute::class

    override val composableContent: @Composable (AnnotationProcessorsNavRoute) -> Unit = { navRoute ->
        AnnotationProcessorsComposable(navRoute)
    }

    object AnnotationProcessorsNavRoute : BaseNavRoute(navPage)
}

@Composable
fun AnnotationProcessorsComposable(
    annotationProcessorsNavRoute: AnnotationProcessorsReportPage.AnnotationProcessorsNavRoute,
    reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
    navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo,
) {
    val allDirectDependenciesCollected by reportDataRepo.allDirectDependencies.collectAsState(null)

    if (allDirectDependenciesCollected == null) {
        BootstrapLoadingSpinner()
        return
    }

    val allDirectDependencies = allDirectDependenciesCollected!!


    val configNameToAnnotationProcessorMap = mutableMapOf<ConfigurationName, MutableSet<DependencyId>>()


    val configurationNameToDependencyIdAndModulePaths =
        mutableMapOf<ConfigurationName, MutableMap<DependencyId, MutableSet<ModulePath>>>()

    val annotationProcessorConfigs = setOf("kapt", "ksp", "annotationProcessor")

    allDirectDependencies.forEach { (gradlePath, configNameToDepIds) ->
        configNameToDepIds.filterKeys { annotationProcessorConfigs.contains(it) }
            .forEach { (configName, dependencyIds) ->
                //
                val curr = configNameToAnnotationProcessorMap[configName] ?: mutableSetOf()
                configNameToAnnotationProcessorMap[configName] = curr.apply { addAll(dependencyIds) }

                // -----
                dependencyIds.forEach { dependencyId ->
                    val currOne = configurationNameToDependencyIdAndModulePaths[configName] ?: mutableMapOf()
                    currOne[dependencyId] = (currOne[dependencyId] ?: mutableSetOf()).apply { add(gradlePath) }
                    configurationNameToDependencyIdAndModulePaths[configName] = currOne
                }
            }
    }

    H1 {
        Text("Annotation Processors")
    }

    if (configurationNameToDependencyIdAndModulePaths.isEmpty()) {
        H3 {
            Text("None found for 'kapt', 'ksp' or 'annotationProcessor'.")
        }
    }

    val expanded = true
    configurationNameToDependencyIdAndModulePaths.forEach { (configurationName, dependencyIdPaths) ->
        BoostrapExpandingCard(
            header = {
                H4 { Text(configurationName) }
            },
            expanded = expanded
        ) {
            dependencyIdPaths.forEach { (dependencyId, gradlePaths) ->
                BootstrapAccordion({
                    Text("$dependencyId (${gradlePaths.size} Modules)")
                }) {
                    BootstrapTable(
                        headers = listOf("Gradle Module"),
                        rows = gradlePaths.map { listOf(it) },
                        types = listOf(String::class),
                        maxResultsLimitConstant = PagingConstants.MAX_RESULTS
                    ) {
                        navRouteRepo.pushNavRoute(
                            ModuleDetailNavRoute(
                                path = it[0]
                            )
                        )
                    }
                }
            }
        }
        Br { }
    }
}
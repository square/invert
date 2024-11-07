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
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H5
import org.jetbrains.compose.web.dom.H6
import org.jetbrains.compose.web.dom.Text
import ui.BootstrapAccordion
import ui.BootstrapLoadingMessageWithSpinner
import ui.BootstrapSettingsCheckbox
import ui.BootstrapTable
import kotlin.reflect.KClass


data class MostTransitiveDependenciesNavRoute(
  val configuration: ConfigurationName? = null,
) : BaseNavRoute(MostTransitiveDependenciesReportPage.navPage) {
  override fun toSearchParams(): Map<String, String> = toParamsWithOnlyPageId(this)
    .apply {
      configuration?.let {configurationName ->
        this[CONFIGURATION_QUERY_PARAM_NAME] = configurationName
      }
    }

  companion object {

    private const val CONFIGURATION_QUERY_PARAM_NAME = "configuration"
    fun parser(params: Map<String, String?>): MostTransitiveDependenciesNavRoute = MostTransitiveDependenciesNavRoute(
      configuration = params[CONFIGURATION_QUERY_PARAM_NAME],
    )
  }
}

object MostTransitiveDependenciesReportPage : InvertReportPage<MostTransitiveDependenciesNavRoute> {
  override val navPage: NavPage = NavPage(
    pageId = "most_transitive_dependencies",
    displayName = "Most Transitive Dependencies",
    navIconSlug = "graph-up-arrow",
    navRouteParser = { params ->
      println("parser")
      MostTransitiveDependenciesNavRoute.parser(params)
    }
  )

  override val navRouteKClass: KClass<MostTransitiveDependenciesNavRoute> = MostTransitiveDependenciesNavRoute::class

  override val composableContent: @Composable (MostTransitiveDependenciesNavRoute) -> Unit = { navRoute ->
    MostTransitiveDependenciesComposable(navRoute)
  }
}

@Composable
fun MostTransitiveDependenciesComposable(
  currentNavRoute: MostTransitiveDependenciesNavRoute,
  reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
  navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo,
) {

//  val moduleToDependenciesMap: Map<ModulePath, Map<ConfigurationName, Set<DependencyId>>>? by reportDataRepo.allDirectDependencies.collectAsState(
//    null
//  )
//  val allConfigurationNames by reportDataRepo.allAnalyzedConfigurationNames.collectAsState(listOf())
//
//  if (moduleToDependenciesMap == null || allConfigurationNames == null) {
//    BootstrapLoadingMessageWithSpinner()
//    return
//  }

  H5 { Text("Configurations") }
//  val currentConfigurationName = currentNavRoute.configuration
//  (allConfigurationNames ?: emptyList()).sorted().forEach { configurationName ->
//    BootstrapSettingsCheckbox(
//      labelText = configurationName,
//      initialIsChecked = currentConfigurationName?.contains(configurationName) == true
//    ) { shouldAdd ->
////      navRouteRepo.updateNavRoute(
////        invertedDependenciesNavRoute.copy(
////          configurations = invertedDependenciesNavRoute.configurations
////            .toMutableList()
////            .apply {
////              remove(configurationName)
////              if (shouldAdd) {
////                add(configurationName)
////              }
////            }
////        )
////      )
//    }
//  }
//
//
//  fun isExcluded(str: String): Boolean {
//    return !str.startsWith(":")
//  }
//
//  val filteredModuleToDependenciesMap = moduleToDependenciesMap?.map { moduleToDependenciesMapEntry ->
//    val gradlePath = moduleToDependenciesMapEntry.key
//    val configurationToDependenciesMap = moduleToDependenciesMapEntry.value
//    val newValues =
//      configurationToDependenciesMap.mapValues { configurationToDependendenciesMapEntry: Map.Entry<ConfigurationName, Set<DependencyId>> ->
//        configurationToDependendenciesMapEntry.value.filter { dependencyId ->
//          !isExcluded(dependencyId)
//        }
//      }
//    gradlePath to newValues
//  }?.toMap() ?: mapOf()
//
//
//  val pathToMaxDirectDependencyCount = filteredModuleToDependenciesMap.mapValues { entry ->
//    entry.value.values.maxOfOrNull { it.size }
//  }.filterKeys { it != "kapt" && it != "annotationProcessor" }
//
//
//  H1 { Text("Leaf Modules") }
//
//  (0..3).forEach { count ->
//    if (pathToMaxDirectDependencyCount.any { it.value == count }) {
//      val modulesWithMaxCount = pathToMaxDirectDependencyCount.filter { it.value == count }
//      BootstrapAccordion(
//        headerContent = {
//          H6 { Text("$count Dependencies (${modulesWithMaxCount.size} Modules)") }
//        },
//        bodyContent = {
//          BootstrapTable(
//            headers = listOf("Module", "Dependencies"),
//            rows = modulesWithMaxCount.map {
//              val gradlePath = it.key
//              val mappings = filteredModuleToDependenciesMap[gradlePath]?.map { entry ->
//                val configurationName = entry.key
//                val dependencyIds = entry.value
//                buildString {
//                  if (dependencyIds.isNotEmpty()) {
//                    appendLine(configurationName)
//                    dependencyIds.forEach { dependencyId ->
//                      appendLine("* $dependencyId")
//                    }
//                  }
//                }
//              }?.joinToString("\n")
//                ?: ""
//              listOf(gradlePath, mappings)
//            },
//            types = listOf(String::class, String::class),
//            maxResultsLimitConstant = PagingConstants.MAX_RESULTS,
//          ) {}
//        })
//    }
//  }
}

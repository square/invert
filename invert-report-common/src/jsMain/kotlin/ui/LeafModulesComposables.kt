package ui

import PagingConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.LeafModulesNavRoute
import com.squareup.invert.models.ConfigurationName
import com.squareup.invert.models.DependencyId
import com.squareup.invert.models.GradlePath
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H6
import org.jetbrains.compose.web.dom.Text

private fun isExcluded(str: String): Boolean {
  return !str.startsWith(":")
}

@Composable
fun LeafModulesComposable(
  reportDataRepo: ReportDataRepo,
  navRouteRepo: NavRouteRepo,
  statsNavRoute: LeafModulesNavRoute
) {

  val moduleToDependenciesMap: Map<GradlePath, Map<ConfigurationName, Set<DependencyId>>>? by reportDataRepo.allDirectDependencies.collectAsState(
    null
  )

  if (moduleToDependenciesMap == null) {
    BootstrapLoadingMessageWithSpinner()
    return
  }

  val filteredModuleToDependenciesMap = moduleToDependenciesMap?.map { moduleToDependenciesMapEntry ->
    val gradlePath = moduleToDependenciesMapEntry.key
    val configurationToDependenciesMap = moduleToDependenciesMapEntry.value
    val newValues =
      configurationToDependenciesMap.mapValues { configurationToDependendenciesMapEntry: Map.Entry<ConfigurationName, Set<DependencyId>> ->
        configurationToDependendenciesMapEntry.value.filter { dependencyId ->
          !isExcluded(dependencyId)
        }
      }
    gradlePath to newValues
  }?.toMap() ?: mapOf()


  val pathToMaxDirectDependencyCount = filteredModuleToDependenciesMap.mapValues { entry ->
    entry.value.values.maxOfOrNull { it.size }
  }.filterKeys { it != "kapt" && it != "annotationProcessor" }


  H1 { Text("Leaf Modules") }

  (0..3).forEach { count ->
    if (pathToMaxDirectDependencyCount.any { it.value == count }) {
      val modulesWithMaxCount = pathToMaxDirectDependencyCount.filter { it.value == count }
      BootstrapAccordion(
        headerContent = {
          H6 { Text("$count Dependencies (${modulesWithMaxCount.size} Modules)") }
        },
        bodyContent = {
          BootstrapTable(
            headers = listOf("Module", "Dependencies"),
            rows = modulesWithMaxCount.map {
              val gradlePath = it.key
              val mappings = filteredModuleToDependenciesMap[gradlePath]?.map { entry ->
                val configurationName = entry.key
                val dependencyIds = entry.value
                buildString {
                  if (dependencyIds.isNotEmpty()) {
                    appendLine(configurationName)
                    dependencyIds.forEach { dependencyId ->
                      appendLine("* $dependencyId")
                    }
                  }
                }
              }?.joinToString("\n")
                ?: ""
              listOf(gradlePath, mappings)
            },
            types = listOf(String::class, String::class),
            maxResultsLimitConstant = PagingConstants.MAX_RESULTS,
          ) {}
        })
    }
  }
}
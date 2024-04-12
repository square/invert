package ui

import PagingConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.AnnotationProcessorsNavRoute
import com.squareup.invert.common.navigation.routes.ModuleDetailNavRoute
import com.squareup.invert.models.ConfigurationName
import com.squareup.invert.models.DependencyId
import com.squareup.invert.models.GradlePath
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.H4
import org.jetbrains.compose.web.dom.Text


@Composable
fun AnnotationProcessorsComposable(
  reportDataRepo: ReportDataRepo,
  navRouteRepo: NavRouteRepo,
  annotationProcessorsNavRoute: AnnotationProcessorsNavRoute
) {
  val allDirectDependenciesCollected by reportDataRepo.allDirectDependencies.collectAsState(null)

  if (allDirectDependenciesCollected == null) {
    BootstrapLoadingSpinner()
    return
  }

  val allDirectDependencies = allDirectDependenciesCollected!!


  val configNameToAnnotationProcessorMap = mutableMapOf<ConfigurationName, MutableSet<DependencyId>>()


  val configurationNameToDependencyIdAndGradlePaths =
    mutableMapOf<ConfigurationName, MutableMap<DependencyId, MutableSet<GradlePath>>>()

  val annotationProcessorConfigs = setOf("kapt", "ksp", "annotationProcessor")

  allDirectDependencies.forEach { (gradlePath, configNameToDepIds) ->
    configNameToDepIds.filterKeys { annotationProcessorConfigs.contains(it) }.forEach { (configName, dependencyIds) ->
      //
      val curr = configNameToAnnotationProcessorMap[configName] ?: mutableSetOf()
      configNameToAnnotationProcessorMap[configName] = curr.apply { addAll(dependencyIds) }

      // -----
      dependencyIds.forEach { dependencyId ->
        val currOne = configurationNameToDependencyIdAndGradlePaths[configName] ?: mutableMapOf()
        currOne[dependencyId] = (currOne[dependencyId] ?: mutableSetOf()).apply { add(gradlePath) }
        configurationNameToDependencyIdAndGradlePaths[configName] = currOne
      }
    }
  }

  H1 {
    Text("Annotation Processors")
  }

  if(configurationNameToDependencyIdAndGradlePaths.isEmpty()){
    H3 {
      Text("None found for 'kapt', 'ksp' or 'annotationProcessor'.")
    }
  }

  val expanded = true
  configurationNameToDependencyIdAndGradlePaths.forEach { (configurationName, dependencyIdPaths) ->
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
            navRouteRepo.updateNavRoute(
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
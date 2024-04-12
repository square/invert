package ui

import PagingConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.UnusedModulesNavRoute
import com.squareup.invert.models.ConfigurationName
import com.squareup.invert.models.DependencyId
import com.squareup.invert.models.GradlePath
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

@Composable
fun UnusedModulesComposable(
  reportDataRepo: ReportDataRepo,
  navRouteRepo: NavRouteRepo,
  statsNavRoute: UnusedModulesNavRoute
) {

  val moduleToInvertedDependenciesMapOrig: Map<DependencyId, Map<GradlePath, List<ConfigurationName>>>? by reportDataRepo.allInvertedDependencies.collectAsState(
    null
  )

  val ownersMapOrig by reportDataRepo.moduleToOwnerMap.collectAsState(null)

  val allAnalyzedConfigurationNamesOrig by reportDataRepo.allAnalyzedConfigurationNames.collectAsState(null)

  val modulesWithAppPluginOrig by reportDataRepo.modulesWithPlugin("com.android.build.gradle.AppPlugin").collectAsState(
    null
  )

  val allModulesOrig: List<GradlePath>? by reportDataRepo.allModules.collectAsState(
    null
  )

  if (moduleToInvertedDependenciesMapOrig == null || allModulesOrig == null) {
    BootstrapLoadingMessageWithSpinner()
    return
  }

  val moduleToInvertedDependenciesMap = moduleToInvertedDependenciesMapOrig ?: mapOf()

  val modulesWithAppPlugin = modulesWithAppPluginOrig?.toSet() ?: setOf()

  val allModules: List<String> = allModulesOrig ?: listOf()

  val allAnalyzedConfigurationNames = allAnalyzedConfigurationNamesOrig ?: setOf()

  val rows = allModules.filterNot { modulesWithAppPlugin.contains(it) }.map {
    val a = moduleToInvertedDependenciesMap[it]
    Pair(it, a?.keys?.size ?: 0)
  }

  val unused = rows.filter { it.second == 0 }

  H1 { Text("Unused Modules (${unused.size} Total)") }
  P {
    Text("* Android Apps are Excluded")
    Br { }
    Text("* Scanned Configurations: $allAnalyzedConfigurationNames")
  }
  BootstrapTable(
    headers = listOf("Module", "Owner"),
    rows = unused.map { listOf(it.first, ownersMapOrig?.get(it.first) ?: "") },
    types = listOf(String::class, String::class),
    maxResultsLimitConstant = PagingConstants.MAX_RESULTS,
  ) {}
}
package ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.ModuleConsumptionNavRoute
import com.squareup.invert.common.utils.DependencyComputations
import com.squareup.invert.models.GradlePath
import com.squareup.invert.models.GradlePluginId
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H5
import org.jetbrains.compose.web.dom.Hr
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

@Composable
fun ModuleConsumptionComposable(
  reportDataRepo: ReportDataRepo,
  navRouteRepo: NavRouteRepo,
  navRoute: ModuleConsumptionNavRoute
) {
  BootstrapTabPane(
    listOf(
      BootstrapTabData("Results") {
        if (navRoute.pluginGroupByFilter.isEmpty() || navRoute.configurations.isEmpty()) {
          BootstrapJumbotron(
            {
              BootstrapIcon("exclamation-triangle", 48)
              Text(" No configuration provided.")
            },
            {
              P({
                classes("text-center")
              }) {
                Text("Plugins and Configuration Names are not selected.  Go to the settings tab, set them and return to this page.")
              }
            })
        } else {
          TitleRow("Module Consumption (Grouped By Plugin Type)")
          val query by reportDataRepo.moduleQuery.collectAsState(null)
          BootstrapSearchBox(query ?: "", "Search For Module...") {
            navRouteRepo.updateNavRoute(
              navRoute.copy(
                moduleQuery = it
              )
            )
          }
          BootstrapRow {
            BootstrapColumn(6) {
              val allCountModules by reportDataRepo.allModules.collectAsState(null)
              val matching by reportDataRepo.allModulesMatchingQuery.collectAsState(null)
              val totalCount = allCountModules?.size
              GenericList(
                "Matching ${matching?.size} of $totalCount",
                matching ?: listOf(),
                onItemClick = {
                  navRouteRepo.updateNavRoute(
                    navRoute.copy(
                      moduleQuery = it
                    )
                  )
                })
            }
            BootstrapColumn(6) {
              RightColumn(reportDataRepo, navRoute)
            }
          }
        }
      },
      BootstrapTabData("Settings") {
        SettingsComposable(
          reportDataRepo = reportDataRepo,
          navRouteRepo = navRouteRepo
        )
      },
    )
  )

}

@Composable
fun SettingsComposable(reportDataRepo: ReportDataRepo, navRouteRepo: NavRouteRepo) {
  val navRoute by navRouteRepo.navRoute.collectAsState(null)
  if (navRoute == null) {
    return
  } else {
    val moduleConsumptionNavRoute = navRoute!! as ModuleConsumptionNavRoute
    val groupByFilterItems = moduleConsumptionNavRoute.pluginGroupByFilter
    H1 {
      Text("Module Consumption Settings")
    }
    Br {}
    BootstrapRow {
      BootstrapColumn(6) {
        H5 { Text("Configurations") }
        val allConfigurationNames by reportDataRepo.allAnalyzedConfigurationNames.collectAsState(listOf())
        BootstrapButton("Select All") {
          navRouteRepo.updateNavRoute(
            moduleConsumptionNavRoute.copy(configurations = allConfigurationNames?.toList() ?: listOf())
          )
        }
        BootstrapButton("Unselect All") {
          navRouteRepo.updateNavRoute(
            moduleConsumptionNavRoute.copy(configurations = listOf())
          )
        }
        allConfigurationNames?.sorted()?.forEach { configurationName ->
          BootstrapSettingsCheckbox(
            labelText = configurationName,
            initialIsChecked = moduleConsumptionNavRoute.configurations.contains(configurationName)
          ) { shouldAdd ->
            navRouteRepo.updateNavRoute(
              moduleConsumptionNavRoute.copy(
                configurations = moduleConsumptionNavRoute.configurations
                  .toMutableList()
                  .apply {
                    remove(configurationName)
                    if (shouldAdd) {
                      add(configurationName)
                    }
                  }
              )
            )
          }
        }
      }
      BootstrapColumn(6) {
        H5 { Text("Plugins") }
        val allPluginIds by reportDataRepo.allPluginIds.collectAsState(listOf())
        BootstrapButton("Select All") {
          navRouteRepo.updateNavRoute(
            moduleConsumptionNavRoute.copy(
              pluginGroupByFilter = allPluginIds ?: listOf()
            )
          )
        }
        BootstrapButton("Unselect All") {
          navRouteRepo.updateNavRoute(
            moduleConsumptionNavRoute.copy(
              pluginGroupByFilter = listOf()
            )
          )
        }
        allPluginIds?.sorted()?.forEach { gradlePluginId ->
          BootstrapSettingsCheckbox(
            labelText = gradlePluginId,
            initialIsChecked = groupByFilterItems.contains(gradlePluginId)
          ) { shouldAdd ->
            navRouteRepo.updateNavRoute(
              moduleConsumptionNavRoute.copy(
                pluginGroupByFilter = moduleConsumptionNavRoute.pluginGroupByFilter.toMutableList()
                  .apply {
                    remove(gradlePluginId)
                    if (shouldAdd) {
                      add(gradlePluginId)
                    }
                  }
              )
            )
          }
        }
      }
    }
  }
}


@Composable
fun RightColumn(
  reportDataRepo: ReportDataRepo,
  moduleConsumptionNavRoute: ModuleConsumptionNavRoute
) {
  val pluginIdToAllAppsMap: Map<GradlePluginId, List<GradlePath>>? by reportDataRepo.pluginIdToAllModulesMap.collectAsState(
    null
  )
  val invertedDeps by reportDataRepo.allInvertedDependencies.collectAsState(null)
  val allModulesMatchingQuery by reportDataRepo.allModulesMatchingQuery.collectAsState(null)
  val collectedPlugins by reportDataRepo.collectedPluginInfoReport.collectAsState(null)
  val pluginIdToGradlePathsMatchingQuery = DependencyComputations.computePluginIdToGradlePathsMatchingQuery(
    matchingQueryModulesList = allModulesMatchingQuery?.filter { it.startsWith(":") } ?: listOf(),
    pluginGroupByFilter = moduleConsumptionNavRoute.pluginGroupByFilter,
    configurations = moduleConsumptionNavRoute.configurations,
    invertedDeps = invertedDeps,
    collectedPlugins = collectedPlugins,
  )

  val limit = 10
  pluginIdToGradlePathsMatchingQuery
    .forEach { (pluginId: GradlePluginId, matchingModules: Map<GradlePath, Map<GradlePath, List<DependencyComputations.PathAndConfigurations>>>) ->
      val totalCount = pluginIdToAllAppsMap?.get(pluginId)?.size ?: 0
      val matchingCount = pluginIdToGradlePathsMatchingQuery[pluginId]?.size ?: 0

      val headerText = "$pluginId ($matchingCount of $totalCount) "
      var showAll by remember { mutableStateOf(false) }
      val matchingModulePaths = matchingModules.keys.toList()
      val expanded = false
      BoostrapExpandingCard(
        header = {
          Text(headerText)
        },
        headerRight = {
          Button({
            classes("btn")
          }) {
            BootstrapIcon("copy", 16) {
              window.navigator.clipboard.writeText(
                matchingModulePaths.joinToString(
                  separator = " "
                )
              )
            }
          }
        },
        expanded = expanded
      ) {
        var displayIndex = 1
        val matchingModulePathsLimited = matchingModulePaths
          .sorted()
          .subList(
            0, minOf(
              if (showAll) {
                Int.MAX_VALUE
              } else {
                limit
              }, matchingModules.size
            )
          )
        matchingModulePathsLimited
          .forEach { matchingModulePath: GradlePath ->
            BootstrapAccordion({
              val accordionSubHeaderText = "$displayIndex $matchingModulePath"
              Text(accordionSubHeaderText)
              displayIndex++
            }) {
              val matchingUsages: List<DependencyComputations.PathAndConfigurations> =
                matchingModules[matchingModulePath]?.get(matchingModulePath) ?: listOf()
              BootstrapTable(
                headers = listOf(
                  "Referenced in",
                  "in Configuration(s)"
                ),
                rows = matchingUsages.map { (gradlePath, configurationNames) ->
                  listOf(
                    gradlePath,
                    configurationNames.toString()
                  )
                },
                types = matchingModulePathsLimited.map { String::class },
                maxResultsLimitConstant = 10,
                onItemClick = null
              )
            }
          }
        if (!showAll) {
          Hr { }
          Button({
            classes("btn")
            onClick {
              showAll = !showAll
            }
          }) {
            Text("Show All")
          }
        }
      }
    }
}
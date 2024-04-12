import com.squareup.af.analysis.navigation.routes.AndroidModuleMetricsLookerDashboardNavRoute
import com.squareup.invert.common.CollectedDataRepo
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavRouteManager
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.DependencyDiffNavRoute
import com.squareup.invert.common.navigation.routes.ModuleConsumptionNavRoute
import kotlinx.browser.window
import kotlinx.coroutines.Dispatchers
import navigation.CustomReport
import navigation.RemoteJsLoadingProgress
import ui.AndroidModuleMetricsDashboard


fun main() {
  val routeManager = NavRouteManager().apply {
    registerDefaultParsers(this)
    registerParser(AndroidModuleMetricsLookerDashboardNavRoute.InvertGithub)
  }

  val initialRoute = routeManager.parseUrlToRoute(window.location.toString())
  val navRouteRepo = NavRouteRepo(initialRoute)

  val reportDataRepo = ReportDataRepo(
    navRoute = navRouteRepo.navRoute,
    collectedDataRepo = CollectedDataRepo(
      coroutineDispatcher = Dispatchers.Default,
      loadFileData = { jsFileKey, reportRepoData ->
        RemoteJsLoadingProgress.loadJavaScriptFile(jsFileKey) { json ->
          RemoteJsLoadingProgress.handleLoadedJsFile(reportRepoData, jsFileKey, json)
        }
      },
    ),
  )

  with(routeManager) {
    registerDefaultInvertNavRoutes(this, reportDataRepo, navRouteRepo)
    registerRoute(AndroidModuleMetricsLookerDashboardNavRoute::class) {
      AndroidModuleMetricsDashboard()
    }
  }

  val customReports = listOf(
    CustomReport(
      text = "What Demo Apps?",
      iconSlug = "question-circle",
      navRoute = ModuleConsumptionNavRoute(
        pluginGroupByFilter = listOf(
          "com.squareup.gradle.DemoAppPlugin"
        ),
        configurations = listOf("debugRuntimeClasspath"),
        moduleQuery = ":features:checkout-v2:"
      ),
    ),
    CustomReport(
      text = "Dependency Diff :invert-models w/js & jvm",
      iconSlug = "question-circle",
      navRoute = DependencyDiffNavRoute(
        moduleA = ":invert-models",
        moduleB = ":invert-models",
        configurationA = "jvmRuntimeClasspath",
        configurationB = "jsRuntimeClasspath",
        includeArtifacts = true,
        showMatching = false,
      ),
    ),
    CustomReport(
      text = "Invert on GitHub",
      iconSlug = "bar-chart",
      navRoute = AndroidModuleMetricsLookerDashboardNavRoute(),
    ),
  )

  invertComposeMain(initialRoute, routeManager, navRouteRepo, customReports)
}

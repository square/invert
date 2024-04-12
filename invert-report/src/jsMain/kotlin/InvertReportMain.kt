import com.squareup.invert.common.CollectedDataRepo
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavRouteManager
import com.squareup.invert.common.navigation.NavRouteRepo
import kotlinx.browser.window
import kotlinx.coroutines.Dispatchers
import navigation.RemoteJsLoadingProgress

fun main() {

  val routeManager = NavRouteManager().apply {
    registerDefaultParsers(this)
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

  registerDefaultInvertNavRoutes(
    navRouteManager = routeManager,
    reportDataRepo = reportDataRepo,
    navRouteRepo = navRouteRepo
  )

  invertComposeMain(
    initialRoute = initialRoute,
    routeManager = routeManager,
    navRouteRepo = navRouteRepo,
    customReports = listOf(),
  )
}
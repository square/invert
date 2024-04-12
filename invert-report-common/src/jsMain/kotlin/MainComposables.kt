import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.common.navigation.NavRouteManager
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.AllModulesNavRoute
import com.squareup.invert.common.navigation.routes.AllStatsNavRoute
import com.squareup.invert.common.navigation.routes.AnnotationProcessorsNavRoute
import com.squareup.invert.common.navigation.routes.ArtifactDetailNavRoute
import com.squareup.invert.common.navigation.routes.ArtifactsNavRoute
import com.squareup.invert.common.navigation.routes.ConfigurationsNavRoute
import com.squareup.invert.common.navigation.routes.DependencyDiffNavRoute
import com.squareup.invert.common.navigation.routes.HomeNavRoute
import com.squareup.invert.common.navigation.routes.LeafModulesNavRoute
import com.squareup.invert.common.navigation.routes.ModuleConsumptionNavRoute
import com.squareup.invert.common.navigation.routes.ModuleDependencyGraphNavRoute
import com.squareup.invert.common.navigation.routes.ModuleDetailNavRoute
import com.squareup.invert.common.navigation.routes.OwnerDetailNavRoute
import com.squareup.invert.common.navigation.routes.OwnersNavRoute
import com.squareup.invert.common.navigation.routes.PluginDetailNavRoute
import com.squareup.invert.common.navigation.routes.PluginsNavRoute
import com.squareup.invert.common.navigation.routes.StatDetailNavRoute
import com.squareup.invert.common.navigation.routes.UnusedModulesNavRoute
import history.JavaScriptNavigationAndHistory
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import navigation.CustomReport
import navigation.LeftNavigationComposable
import navigation.RemoteJsLoadingProgress
import org.jetbrains.compose.web.renderComposable
import ui.AllStatsComposable
import ui.AnnotationProcessorsComposable
import ui.ArtifactDetailComposable
import ui.ArtifactsComposable
import ui.ConfigurationsComposable
import ui.HomeComposable
import ui.LeafModulesComposable
import ui.ModuleConsumptionComposable
import ui.ModuleDependencyDiffComposable
import ui.ModuleDependencyGraphComposable
import ui.ModuleDetailComposable
import ui.ModulesComposable
import ui.NavBarComposable
import ui.OwnerDetailComposable
import ui.OwnersComposable
import ui.PluginDetailComposable
import ui.PluginsComposable
import ui.StatDetailComposable
import ui.UnusedModulesComposable


fun invertComposeMain(
  initialRoute: NavRoute,
  routeManager: NavRouteManager,
  navRouteRepo: NavRouteRepo,
  customReports: List<CustomReport>
) {
  setupNavigation(routeManager, navRouteRepo)

  renderComposable(rootElementId = "navigation") {
    LeftNavigationComposable(initialRoute, navRouteRepo, customReports)
  }

  renderComposable(rootElementId = "main_content") {
    MainContentComposable(routeManager, navRouteRepo)
  }

  renderComposable(rootElementId = "navbar_content") {
    NavBarComposable(RemoteJsLoadingProgress.awaitingResults)
  }
}

@OptIn(DelicateCoroutinesApi::class)
fun setupNavigation(routeManager: NavRouteManager, navRouteRepo: NavRouteRepo) {
  // Update Route on Every Change
  navRouteRepo.navRoute.onEach {
    JavaScriptNavigationAndHistory.setUrlFromNavRoute(it)
  }.launchIn(GlobalScope)

  // Register for Browser Back/Forward Button Events
  JavaScriptNavigationAndHistory.registerForPopstate(routeManager, navRouteRepo)
}

fun registerDefaultParsers(
  navRouteManager: NavRouteManager,
) {
  with(navRouteManager) {
    registerParser(AllModulesNavRoute.AllModules)
    registerParser(LeafModulesNavRoute.LeafModules)
    registerParser(ModuleDetailNavRoute.ModuleDetail)
    registerParser(DependencyDiffNavRoute.DependencyDiff)
    registerParser(OwnersNavRoute.Owners)
    registerParser(OwnerDetailNavRoute.OwnerDetail)
    registerParser(ArtifactsNavRoute.Artifacts)
    registerParser(ArtifactDetailNavRoute.ArtifactDetail)
    registerParser(AnnotationProcessorsNavRoute.AnnotationProcessors)
    registerParser(PluginsNavRoute.Plugins)
    registerParser(PluginDetailNavRoute.PluginDetail)
    registerParser(StatDetailNavRoute.StatDetail)
    registerParser(AllStatsNavRoute.AllStats)
    registerParser(ModuleConsumptionNavRoute.ModuleConsumption)
    registerParser(ArtifactsNavRoute.Artifacts)
    registerParser(ConfigurationsNavRoute.Configurations)
    registerParser(ModuleDependencyGraphNavRoute.ModuleDependencyGraph)
    registerParser(UnusedModulesNavRoute.UnusedModules)
  }
}

fun registerDefaultInvertNavRoutes(
  navRouteManager: NavRouteManager,
  reportDataRepo: ReportDataRepo,
  navRouteRepo: NavRouteRepo
) {
  with(navRouteManager) {
    registerRoute(HomeNavRoute::class) { HomeComposable(reportDataRepo, navRouteRepo) }
    registerRoute(OwnersNavRoute::class) { OwnersComposable(reportDataRepo, navRouteRepo) }
    registerRoute(OwnerDetailNavRoute::class) { OwnerDetailComposable(reportDataRepo, navRouteRepo, it) }
    registerRoute(UnusedModulesNavRoute::class) { UnusedModulesComposable(reportDataRepo, navRouteRepo, it) }
    registerRoute(LeafModulesNavRoute::class) { LeafModulesComposable(reportDataRepo, navRouteRepo, it) }
    registerRoute(AllModulesNavRoute::class) { ModulesComposable(reportDataRepo, navRouteRepo, it) }
    registerRoute(AnnotationProcessorsNavRoute::class) {
      AnnotationProcessorsComposable(
        reportDataRepo,
        navRouteRepo,
        it
      )
    }
    registerRoute(DependencyDiffNavRoute::class) {
      ModuleDependencyDiffComposable(
        reportDataRepo,
        navRouteRepo,
        it
      )
    }
    registerRoute(ModuleDetailNavRoute::class) { ModuleDetailComposable(reportDataRepo, navRouteRepo, it) }
    registerRoute(StatDetailNavRoute::class) { StatDetailComposable(reportDataRepo, navRouteRepo, it) }
    registerRoute(AllStatsNavRoute::class) { AllStatsComposable(reportDataRepo, navRouteRepo, it) }
    registerRoute(ArtifactsNavRoute::class) { ArtifactsComposable(reportDataRepo, navRouteRepo, it) }
    registerRoute(ArtifactDetailNavRoute::class) { ArtifactDetailComposable(reportDataRepo, navRouteRepo, it) }
    registerRoute(PluginsNavRoute::class) { PluginsComposable(reportDataRepo, navRouteRepo) }
    registerRoute(PluginDetailNavRoute::class) { PluginDetailComposable(reportDataRepo, navRouteRepo, it) }
    registerRoute(ConfigurationsNavRoute::class) { ConfigurationsComposable(reportDataRepo) }
    registerRoute(ModuleConsumptionNavRoute::class) {
      ModuleConsumptionComposable(
        reportDataRepo,
        navRouteRepo,
        it
      )
    }
    registerRoute(ModuleDependencyGraphNavRoute::class) { ModuleDependencyGraphComposable(reportDataRepo, navRouteRepo, it) }
  }
}

@Composable
fun MainContentComposable(
  navRouteManager: NavRouteManager,
  navRouteRepo: NavRouteRepo
) {
  val navRouteCollected = navRouteRepo.navRoute.collectAsState(null)
  navRouteCollected.value?.let { navRoute ->
    navRouteManager.renderContentForRoute(navRoute)
  }
}

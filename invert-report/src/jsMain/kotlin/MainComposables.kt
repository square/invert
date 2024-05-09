import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.common.navigation.NavRouteManager
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.*
import history.JavaScriptNavigationAndHistory
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import navigation.CustomNavItem
import navigation.LeftNavigationComposable
import navigation.RemoteJsLoadingProgress
import org.jetbrains.compose.web.renderComposable
import ui.*


fun invertComposeMain(
    initialRoute: NavRoute,
    routeManager: NavRouteManager,
    navRouteRepo: NavRouteRepo,
    customNavItems: List<CustomNavItem>
) {
    setupNavigation(routeManager, navRouteRepo)

    renderComposable(rootElementId = "navigation") {
        LeftNavigationComposable(initialRoute, navRouteRepo, customNavItems)
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

fun registerDefaultNavPageParsers(
    navRouteManager: NavRouteManager,
) {
    with(navRouteManager) {
        registerParser(PluginsNavRoute.navPage)
        registerParser(PluginDetailNavRoute("").navPage)
        registerParser(StatDetailNavRoute(emptyList(), emptyList()).navPage)
        registerParser(ModuleDependencyGraphNavRoute().navPage)
        registerParser(UnusedModulesNavRoute().navPage) // TODO
    }
}

fun registerDefaultInvertNavRoutes(
    navRouteManager: NavRouteManager,
    reportDataRepo: ReportDataRepo,
    navRouteRepo: NavRouteRepo
) {
    with(navRouteManager) {
        registerRoute(UnusedModulesNavRoute::class) { UnusedModulesComposable(reportDataRepo, navRouteRepo, it) }
        registerRoute(StatDetailNavRoute::class) { StatDetailComposable(reportDataRepo, navRouteRepo, it) }
        registerRoute(PluginsNavRoute::class) { PluginsComposable(reportDataRepo, navRouteRepo) }
        registerRoute(PluginDetailNavRoute::class) { PluginDetailComposable(reportDataRepo, navRouteRepo, it) }
        registerRoute(ModuleDependencyGraphNavRoute::class) {
            ModuleDependencyGraphComposable(
                reportDataRepo,
                navRouteRepo,
                it
            )
        }
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

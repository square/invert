import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.common.navigation.NavRouteManager
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.*
import history.JavaScriptNavigationAndHistory
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.html.ATarget
import navigation.CustomNavItem
import navigation.LeftNavigationComposable
import navigation.RemoteJsLoadingProgress
import org.jetbrains.compose.web.attributes.ATarget.*
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable
import ui.*


fun invertComposeMain(
    initialRoute: NavRoute,
    routeManager: NavRouteManager,
    navRouteRepo: NavRouteRepo,
    customNavItems: List<CustomNavItem>,
    reportDataRepo: ReportDataRepo,
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

    renderComposable(rootElementId = "navbar_title") {
        val reportMetadata by reportDataRepo.reportMetadata.collectAsState(null)

        Text("Invert Report")
        reportMetadata?.let { metadata ->
            Text(" for ")
            A(href = metadata.remoteRepoUrl, attrs = { target(Blank) }) {
                Text(metadata.remoteRepoUrl.substringAfter("//").substringAfter("/"))
            }
        }
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

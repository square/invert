import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavGroupsRepo
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.common.navigation.NavRouteManager
import com.squareup.invert.common.navigation.NavRouteRepo
import history.JavaScriptNavigationAndHistory
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import navigation.LeftNavigationComposable
import navigation.RemoteJsLoadingProgress
import org.jetbrains.compose.web.attributes.ATarget.Blank
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable
import ui.NavBarComposable


fun invertComposeMain(
  initialRoute: NavRoute,
  routeManager: NavRouteManager,
  navRouteRepo: NavRouteRepo,
  reportDataRepo: ReportDataRepo,
  navGroupsRepo: NavGroupsRepo,
) {
  setupNavigation(routeManager, navRouteRepo)

  renderComposable(rootElementId = "navigation") {
    LeftNavigationComposable(initialRoute, navRouteRepo, reportDataRepo, navGroupsRepo)
  }

  renderComposable(rootElementId = "main_content") {
    MainContentComposable(routeManager, navRouteRepo)
  }

  renderComposable(rootElementId = "navbar_content") {
    NavBarComposable(RemoteJsLoadingProgress.awaitingResults)
  }

  renderComposable(rootElementId = "navbar_title") {
    val reportMetadata by reportDataRepo.reportMetadata.collectAsState(null)

    Text("\uD83D\uDD03 Invert Report")
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
  val javaScriptNavigationAndHistory = JavaScriptNavigationAndHistory(routeManager, navRouteRepo)
  // Update Route on Every Change
  navRouteRepo.navRoute.onEach { navRouteEvent ->
    JavaScriptNavigationAndHistory.setUrlFromNavRoute(
      navRoute = navRouteEvent.navRoute,
      pushOrReplaceState = navRouteEvent.pushOrReplaceState,
    )
  }.launchIn(GlobalScope)

  // Register for Browser Back/Forward Button Events
  javaScriptNavigationAndHistory.registerForPopstate()
}

@Composable
fun MainContentComposable(
  navRouteManager: NavRouteManager,
  navRouteRepo: NavRouteRepo
) {
  val navRouteCollected = navRouteRepo.navRoute.collectAsState(null)
  navRouteCollected.value?.let { navRouteEvent ->
    navRouteManager.renderContentForRoute(navRouteEvent.navRoute)
  }
}

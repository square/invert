package navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.common.navigation.NavRouteRepo
import ui.BootstrapNavItem
import ui.BootstrapNavSectionHeader

data class CustomNavItem(
  val text: String,
  val iconSlug: String,
  val navRoute: NavRoute
)

@Composable
fun LeftNavigationComposable(initialRoute: NavRoute, navRouteRepo: NavRouteRepo, customReports: List<CustomNavItem>) {
  /** Entry Point */
  val navRoute by navRouteRepo.navRoute.collectAsState(initialRoute)
  NavPage
    .ROOT_NAV_ITEMS
    .also {
      println(it.size)
      println(it.map { it?.pageId })
    }
//    .filterNotNull()
    .also {
      println(it.size)
      println(it.map { it?.pageId })
    }
    .forEach { rootNavItem: NavPage ->
      println(rootNavItem)
      val displayName = rootNavItem.displayName
      val navIconSlug = rootNavItem.navIconSlug
      val newNavRoute = rootNavItem.navRouteParser(mapOf())
      BootstrapNavItem(
        text = displayName,
        iconSlug = navIconSlug,
        activeTab = navRoute::class == newNavRoute::class,
        onClick = {
          navRouteRepo.updateNavRoute(newNavRoute)
        }
      )
    }

  if (customReports.isNotEmpty()) {
    BootstrapNavSectionHeader("Custom Reports", "fire")

    customReports.forEach {
      BootstrapNavItem(
        text = it.text,
        iconSlug = it.iconSlug,
        onClick = { navRouteRepo.updateNavRoute(it.navRoute) }
      )
    }
  }
}

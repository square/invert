package ui

import androidx.compose.runtime.Composable
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.common.navigation.routes.toQueryString
import org.jetbrains.compose.web.dom.A

/**
 * Creates an "a" element link for a [NavRoute].
 */
@Composable
fun NavRouteLink(
  navRoute: NavRoute,
  updateNavRoute: (NavRoute) -> Unit,
  content: @Composable () -> Unit,
) {
  A(
    navRoute.toQueryString(),
    {
      onClick { event ->
        if (event.ctrlKey || event.metaKey) {
          // If it's a ctrl-click, allow user to open in new tab
          return@onClick
        }

        event.preventDefault()
        updateNavRoute(navRoute)
      }
    }
  ) {
    content()
  }
}

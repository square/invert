package ui

import androidx.compose.runtime.Composable
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.common.navigation.routes.toQueryString
import org.jetbrains.compose.web.dom.A

@Composable
fun NavRouteLink(
  navRoute: NavRoute,
  updateNavRoute: (NavRoute) -> Unit,
  content: @Composable () -> Unit,
) {
  A(
    navRoute.toQueryString(),
    {
      onClick {
        it.preventDefault()
        updateNavRoute(navRoute)
      }
    }) {
    content()
  }
}

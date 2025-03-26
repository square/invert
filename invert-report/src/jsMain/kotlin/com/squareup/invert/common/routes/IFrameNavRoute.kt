package com.squareup.invert.common.routes

import androidx.compose.runtime.Composable
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.common.routes.IFrameNavRoute.Companion.URL_PARAM_KEY
import org.jetbrains.compose.web.dom.Text
import ui.BootstrapJumbotron
import ui.FullScreenIframe
import kotlin.reflect.KClass

data class IFrameNavRoute(
  val url: String?,
) : BaseNavRoute(IFrameReportPage.navPage) {
  override fun toSearchParams(): Map<String, String> {
    val params = super.toSearchParams().toMutableMap()
    url?.let { params[URL_PARAM_KEY] = it }
    return params
  }

  companion object {
    const val URL_PARAM_KEY = "url"
  }
}

object IFrameReportPage : InvertReportPage<IFrameNavRoute> {
  override val showInNav: Boolean = false
  override val navPage: NavPage = NavPage(
    pageId = "iframe",
    displayName = "IFrame",
    navIconSlug = "file-earmark-bar-graph",
    navRouteParser = { params: Map<String, String?> ->
      val url = params[URL_PARAM_KEY]
      IFrameNavRoute(url)
    }
  )

  override val navRouteKClass: KClass<IFrameNavRoute> = IFrameNavRoute::class

  override val composableContent: @Composable (IFrameNavRoute) -> Unit = { navRoute ->
    if (navRoute.url != null) {
      FullScreenIframe(navRoute.url)
    } else {
      BootstrapJumbotron(centered = true, headerContent = {}) {
        Text("No Url Specified")
      }
    }
  }
}

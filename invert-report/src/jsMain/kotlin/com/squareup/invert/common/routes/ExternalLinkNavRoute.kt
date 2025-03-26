package com.squareup.invert.common.routes

import androidx.compose.runtime.Composable
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.common.pages.HomeReportPage
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Text
import ui.BootstrapJumbotron
import kotlin.reflect.KClass

data class ExternalLinkNavRoute(
  val url: String,
  val title: String?,
) : BaseNavRoute(ExternalLinkReportPage.navPage) {
  override fun toSearchParams(): Map<String, String> {
    val params = super.toSearchParams().toMutableMap()
    title?.let { params[TITLE_PARAM_KEY] = it }
    params[URL_PARAM_KEY] = url
    return params
  }

  companion object {
    const val TITLE_PARAM_KEY = "title"
    const val URL_PARAM_KEY = "url"
  }
}

object ExternalLinkReportPage : InvertReportPage<ExternalLinkNavRoute> {
  override val showInNav: Boolean = false
  override val navPage: NavPage = NavPage(
    pageId = "external_link",
    displayName = "External Link",
    navIconSlug = "link",
    navRouteParser = { params: Map<String, String?> ->
      val url = params[ExternalLinkNavRoute.Companion.URL_PARAM_KEY]
      val title = params[ExternalLinkNavRoute.Companion.TITLE_PARAM_KEY]
      if (url != null) {
        ExternalLinkNavRoute(
          title = title,
          url = url
        )
      } else {
        HomeReportPage.HomeNavRoute
      }
    }
  )

  override val navRouteKClass: KClass<ExternalLinkNavRoute> = ExternalLinkNavRoute::class

  override val composableContent: @Composable (ExternalLinkNavRoute) -> Unit = { navRoute ->
    BootstrapJumbotron(
      centered = true,
      headerContent = {
        A(
          href = navRoute.url,
          attrs = {
            onClick {
              it.preventDefault()
              window.open(url = navRoute.url, target = "_blank")
            }
          }
        ) {
          Text(navRoute.title ?: "External Link")
        }
      }) {
      Text("Click above to visit the External Link")
    }
  }
}

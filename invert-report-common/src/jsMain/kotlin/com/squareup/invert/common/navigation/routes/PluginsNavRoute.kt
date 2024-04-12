package com.squareup.invert.common.navigation.routes

import com.squareup.invert.common.navigation.NavPage

class PluginsNavRoute : BaseNavRoute(Plugins.pageId) {
  companion object {
    val Plugins = NavPage(
      pageId = "plugins",
      displayName = "Plugins",
      navIconSlug = "plugin",
      navRouteParser = { PluginsNavRoute() }
    )
  }
}

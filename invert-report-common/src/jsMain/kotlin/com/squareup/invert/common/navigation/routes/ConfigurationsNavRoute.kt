package com.squareup.invert.common.navigation.routes

import com.squareup.invert.common.navigation.NavPage

class ConfigurationsNavRoute : BaseNavRoute(Configurations.pageId) {
  companion object {
    val Configurations = NavPage(
      pageId = "configurations",
      displayName = "Configurations",
      navIconSlug = "gear",
      navRouteParser = { ConfigurationsNavRoute() }
    )
  }
}

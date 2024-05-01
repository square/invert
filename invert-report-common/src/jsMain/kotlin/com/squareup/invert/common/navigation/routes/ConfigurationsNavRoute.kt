package com.squareup.invert.common.navigation.routes

import com.squareup.invert.common.navigation.NavPage

object ConfigurationsNavRoute : BaseNavRoute(NavPage(
  pageId = "configurations",
  displayName = "Configurations",
  navIconSlug = "gear",
  navRouteParser = { ConfigurationsNavRoute }
))
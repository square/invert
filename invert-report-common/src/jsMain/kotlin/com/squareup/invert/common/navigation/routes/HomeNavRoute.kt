package com.squareup.invert.common.navigation.routes

import com.squareup.invert.common.navigation.NavPage

class HomeNavRoute : BaseNavRoute(Home.pageId) {
  companion object {
    val Home = NavPage(
      pageId = "home",
      displayName = "Home",
      navIconSlug = "house",
      navRouteParser = { HomeNavRoute() }
    )
  }
}

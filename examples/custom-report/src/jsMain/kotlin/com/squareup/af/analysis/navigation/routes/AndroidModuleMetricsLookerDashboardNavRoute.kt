package com.squareup.af.analysis.navigation.routes

import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.routes.BaseNavRoute

class AndroidModuleMetricsLookerDashboardNavRoute : BaseNavRoute(InvertGithub.pageId) {
  companion object {
    val InvertGithub = NavPage(
      pageId = "invert_github",
      displayName = "Invert on GitHub",
      navIconSlug = "file-earmark-bar-graph",
      navRouteParser = { AndroidModuleMetricsLookerDashboardNavRoute() }
    )
  }
}

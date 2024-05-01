package com.squareup.af.analysis.navigation.routes

import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.routes.BaseNavRoute

object InvertOnGithubNavRoute : BaseNavRoute(NavPage(
    pageId = "invert_github",
    displayName = "Invert on GitHub",
    navIconSlug = "file-earmark-bar-graph",
    navRouteParser = { InvertOnGithubNavRoute }
))
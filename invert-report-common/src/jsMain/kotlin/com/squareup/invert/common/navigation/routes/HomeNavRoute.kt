package com.squareup.invert.common.navigation.routes

import com.squareup.invert.common.navigation.NavPage


object HomeNavRoute : BaseNavRoute(NavPage(
    pageId = "home",
    displayName = "Home",
    navIconSlug = "house",
    navRouteParser = { HomeNavRoute }
))
package com.squareup.invert.common.navigation.routes

import com.squareup.invert.common.navigation.NavPage

object OwnersNavRoute : BaseNavRoute(NavPage(
    pageId = "owners",
    displayName = "Owners",
    navIconSlug = "people",
    navRouteParser = { OwnersNavRoute }
))

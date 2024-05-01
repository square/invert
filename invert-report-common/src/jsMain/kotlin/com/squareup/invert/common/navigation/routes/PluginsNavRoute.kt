package com.squareup.invert.common.navigation.routes

import com.squareup.invert.common.navigation.NavPage

object PluginsNavRoute : BaseNavRoute(
    NavPage(
        pageId = "plugins",
        displayName = "Plugins",
        navIconSlug = "plugin",
        navRouteParser = { PluginsNavRoute },
    ),
)


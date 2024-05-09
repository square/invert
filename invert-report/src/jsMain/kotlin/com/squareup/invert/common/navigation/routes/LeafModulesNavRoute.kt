package com.squareup.invert.common.navigation.routes

import com.squareup.invert.common.navigation.NavPage


object LeafModulesNavRoute : BaseNavRoute(NavPage(
    pageId = "leaf_modules",
    displayName = "Leaf Modules",
    navIconSlug = "tree",
    navRouteParser = {
        LeafModulesNavRoute
    }
)) {
    override fun toSearchParams() = toParamsWithOnlyPageId(this)
}

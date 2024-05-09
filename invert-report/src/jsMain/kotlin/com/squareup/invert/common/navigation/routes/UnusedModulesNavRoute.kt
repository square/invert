package com.squareup.invert.common.navigation.routes

import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRoute


class UnusedModulesNavRoute : BaseNavRoute(
    NavPage(
        pageId = "unused_modules",
        displayName = "Unused Modules",
        navIconSlug = "trash",
        navRouteParser = {
            UnusedModulesNavRoute.parser(it)
        }
    )
) {
    override fun toSearchParams() = toParamsWithOnlyPageId(this)

    companion object {
        fun parser(params: Map<String, String?>): NavRoute {
            return UnusedModulesNavRoute()
        }
    }
}

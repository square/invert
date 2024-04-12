package com.squareup.invert.common.navigation.routes

import com.squareup.invert.common.navigation.NavPage

class OwnersNavRoute : BaseNavRoute(Owners.pageId) {
    companion object {
        val Owners = NavPage(
            pageId = "owners",
            displayName = "Owners",
            navIconSlug = "people",
            navRouteParser = { OwnersNavRoute() }
        )
    }
}

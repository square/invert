package com.squareup.invert.common.pages

import androidx.compose.runtime.Composable
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import ui.HomeComposable
import kotlin.reflect.KClass

object HomeReportPage : InvertReportPage<HomeReportPage.HomeNavRoute> {
    override val navPage: NavPage = NavPage(
        pageId = "home",
        displayName = "Home",
        navIconSlug = "house",
        navRouteParser = { HomeNavRoute }
    )
    override val navRouteKClass: KClass<HomeNavRoute> = HomeNavRoute::class

    override val composableContent: @Composable (HomeNavRoute) -> Unit = { navRoute ->
        HomeComposable(navRoute)
    }

    object HomeNavRoute : BaseNavRoute(navPage)
}

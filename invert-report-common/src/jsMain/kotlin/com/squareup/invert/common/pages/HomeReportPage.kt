package com.squareup.invert.common.pages

import androidx.compose.runtime.Composable
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import ui.HomeComposable
import kotlin.reflect.KClass

class HomeReportPage(
    val reportDataRepo: ReportDataRepo,
    val navRouteRepo: NavRouteRepo
) : InvertReportPage<HomeReportPage.HomeNavRoute> {
    override val navPage: NavPage = NAV_PAGE
    override val navRouteKClass: KClass<HomeNavRoute> = HomeNavRoute::class

    override val composableContent: @Composable (HomeNavRoute) -> Unit = { navRoute ->
        HomeComposable(reportDataRepo, navRouteRepo)
    }

    companion object {
        val NAV_PAGE = NavPage(
            pageId = "home",
            displayName = "Home",
            navIconSlug = "house",
            navRouteParser = { HomeNavRoute }
        )
    }

    object HomeNavRoute : BaseNavRoute(HomeReportPage.NAV_PAGE)
}

package com.squareup.invert.common.navigation

import androidx.compose.runtime.Composable
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.common.navigation.routes.HomeNavRoute
import history.JavaScriptNavigationAndHistory.toMap
import org.w3c.dom.url.URL
import kotlin.reflect.KClass
import kotlin.reflect.cast

class NavRouteManager {
    private val routes = mutableMapOf<KClass<out NavRoute>, @Composable (NavRoute) -> Unit>()

    private val navRouteParsers = mutableMapOf<NavPageId, NavPage>()


    fun <T : NavRoute> registerRoute(
        clazz: KClass<T>,
        content: @Composable (T) -> Unit
    ) {
        routes[clazz] = { navRoute ->
            content(clazz.cast(navRoute))
        }
    }

    fun registerParser(navPage: NavPage) {
        navRouteParsers[navPage.pageId] = navPage
    }

    fun parseUrlToRoute(url: String): NavRoute {
        val params = URL(url).searchParams.toMap()
        return parseParamsToRoute(params)
    }

    fun parseParamsToRoute(params: Map<String, String?>): NavRoute {
        val pageId = params[BaseNavRoute.PAGE_ID_PARAM]
        pageId?.let {
            navRouteParsers[pageId]?.navRouteParser?.invoke(params)?.let {
                return it
            }
        }
        return HomeNavRoute
    }


    @Composable
    fun renderContentForRoute(navRoute: NavRoute) {
        routes[navRoute::class]!!.invoke(navRoute)
    }

}

package com.squareup.invert.common

import androidx.compose.runtime.Composable
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.common.navigation.NavRouteManager
import com.squareup.invert.common.navigation.NavRouteRepo
import invertComposeMain
import kotlinx.browser.window
import kotlinx.coroutines.Dispatchers
import navigation.CustomNavItem
import navigation.RemoteJsLoadingProgress
import registerDefaultInvertNavRoutes
import registerDefaultNavPageParsers
import kotlin.reflect.KClass
import kotlin.reflect.cast


class InvertReportPage<T : NavRoute>(
    val navPage: NavPage,
    val navRouteKClass: KClass<T>,
    val composableContent: @Composable (NavRoute) -> Unit,
)

class InvertReport(
    customNavItems: List<CustomNavItem> = emptyList(),
    reportPages: List<InvertReportPage<out NavRoute>> = emptyList(),
) {
    private val routeManager = NavRouteManager()

    init {
        registerDefaultNavPageParsers(routeManager)
        reportPages.forEach { reportPage ->
            routeManager.registerParser(reportPage.navPage)
        }
        reportPages.forEach { reportPage ->
            routeManager.registerRoute(
                clazz = reportPage.navRouteKClass,
                content = {
                    reportPage.composableContent(reportPage.navRouteKClass.cast(it))
                }
            )
        }
//        customComposables.entries.forEach { (key, value) ->
//            routeManager.registerRoute(key, value)
//        }
    }

    private val initialRoute: NavRoute = routeManager.parseUrlToRoute(window.location.toString())

    val navRouteRepo = NavRouteRepo(initialRoute)


    val reportDataRepo = ReportDataRepo(
        navRoute = navRouteRepo.navRoute,
        collectedDataRepo = CollectedDataRepo(
            coroutineDispatcher = Dispatchers.Default,
            loadFileData = { jsFileKey, reportRepoData ->
                RemoteJsLoadingProgress.loadJavaScriptFile(jsFileKey) { json ->
                    RemoteJsLoadingProgress.handleLoadedJsFile(reportRepoData, jsFileKey, json)
                }
            },
        ),
    )

    init {
        registerDefaultInvertNavRoutes(
            navRouteManager = routeManager,
            reportDataRepo = reportDataRepo,
            navRouteRepo = navRouteRepo
        )
        invertComposeMain(
            initialRoute = initialRoute,
            routeManager = routeManager,
            navRouteRepo = navRouteRepo,
            customNavItems = customNavItems,
        )
    }
}
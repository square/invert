package com.squareup.invert.common

import androidx.compose.runtime.Composable
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.common.navigation.NavRouteManager
import com.squareup.invert.common.navigation.NavRouteRepo
import invertComposeMain
import kotlinx.browser.window
import kotlinx.coroutines.Dispatchers
import navigation.CustomNavPage
import navigation.RemoteJsLoadingProgress
import registerDefaultInvertNavRoutes
import registerDefaultParsers
import kotlin.reflect.KClass

class InvertReport(
    customPages: List<NavPage> = emptyList(),
    customNavItems: List<CustomNavPage> = emptyList(),
    customComposables: Map<KClass<*>, @Composable () -> Unit> = emptyMap(),
) {
    private val routeManager = NavRouteManager()

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
        routeManager.apply {
            registerDefaultParsers(this)
            customPages.forEach {
                registerParser(it)
            }
        }
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
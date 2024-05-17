package com.squareup.invert.common

import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.common.navigation.NavRouteManager
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.pages.*
import invertComposeMain
import kotlinx.browser.window
import kotlinx.coroutines.Dispatchers
import navigation.CustomNavItem
import navigation.RemoteJsLoadingProgress

class InvertReport(
    customNavItems: List<CustomNavItem> = emptyList(),
    customReportPages: List<InvertReportPage<out NavRoute>> = emptyList(),
) {
    private val routeManager = NavRouteManager()

    init {
        val allReportPages = DEFAULT_REPORT_PAGES + customReportPages
        allReportPages.forEach { reportPage ->
            routeManager.registerParser(reportPage.navPage)
        }
        allReportPages.forEach { reportPage ->
            routeManager.registerRoute(
                clazz = reportPage.navRouteKClass,
                content = {
                    reportPage.composableContentWithRouteCast(it)
                }
            )
        }
    }

    private val initialRoute: NavRoute = routeManager.parseUrlToRoute(window.location.toString())

    val navRouteRepo = NavRouteRepo(initialRoute)

    val collectedDataRepo = CollectedDataRepo(
        coroutineDispatcher = Dispatchers.Default,
        loadFileData = { jsFileKey, reportRepoData ->
            RemoteJsLoadingProgress.loadJavaScriptFile(jsFileKey) { json ->
                RemoteJsLoadingProgress.handleLoadedJsFile(reportRepoData, jsFileKey, json)
            }
        },
    )

    val reportDataRepo = ReportDataRepo(
        navRoute = navRouteRepo.navRoute,
        collectedDataRepo = collectedDataRepo,
    )

    init {
        invertComposeMain(
            initialRoute = initialRoute,
            routeManager = routeManager,
            navRouteRepo = navRouteRepo,
            customNavItems = customNavItems,
            reportDataRepo = reportDataRepo,
        )
    }

    init {
        // REALLY HACKY Static DI Graph
        DependencyGraph.initialize(
            collectedDataRepo = collectedDataRepo,
            navRouteRepo = navRouteRepo,
            reportDataRepo = reportDataRepo,
        )
    }

    companion object {
        private val DEFAULT_REPORT_PAGES = listOf<InvertReportPage<*>>(
            AllModulesReportPage,
            AllStatsReportPage,
            ArtifactDetailReportPage,
            ArtifactsReportPage,
            AnnotationProcessorsReportPage,
            ConfigurationDetailReportPage,
            ConfigurationsReportPage,
            DependencyInjectionReportPage,
            DependencyDiffReportPage,
            ModuleDependencyGraphReportPage,
            HomeReportPage,
            KotlinCompilerPluginsReportPage,
            LeafModulesReportPage,
            ModuleDetailReportPage,
            InvertedDependenciesReportPage,
            OwnerDetailReportPage,
            OwnersReportPage,
            PluginDetailReportPage,
            PluginsReportPage,
            StatDetailReportPage,
            UnusedModulesReportPage,
        )
    }
}

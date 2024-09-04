package com.squareup.invert.common.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.CollectedDataRepo
import com.squareup.invert.common.DependencyGraph
import com.squareup.invert.common.DiProvidesAndInjectsItem
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.models.ModulePath
import highlightJsHighlightAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.dom.Code
import org.jetbrains.compose.web.dom.Datalist
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Hr
import org.jetbrains.compose.web.dom.Li
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.Pre
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Ul
import ui.BootstrapLoadingMessageWithSpinner
import ui.BootstrapSearchBox
import kotlin.reflect.KClass


data class HotwireNavRoute(
    val path: ModulePath? = null,
) : BaseNavRoute(HotwireReportPage.navPage) {
    override fun toSearchParams() = toParamsWithOnlyPageId(this)
        .also { map ->
            path?.let {
                map[PATH_PARAM] = path
            }
        }

    companion object {
        private const val PATH_PARAM = "path"

        fun parser(params: Map<String, String?>): NavRoute {
            println("PARSE ${params}")
            return HotwireNavRoute(
                path = params[PATH_PARAM],
            )
        }
    }
}

object HotwireReportPage : InvertReportPage<HotwireNavRoute> {
    override val navPage: NavPage = NavPage(
        pageId = "hotwire",
        displayName = "Hotwire",
        navIconSlug = "car-front-fill",
        navRouteParser = { HotwireNavRoute.parser(it) }
    )
    override val navRouteKClass: KClass<HotwireNavRoute> = HotwireNavRoute::class

    override val composableContent: @Composable (HotwireNavRoute) -> Unit = { navRoute ->
        HotwireComposable(navRoute)
    }

}

// :impl (Selected Module)
// Injects com.android.Application with MyApplication from :app-provider
// Injects com.Metrics with com.MetricsImpl from :impl-metrics
//
// :app-provider
// Injects com.Logger with com.LoggerImpl from :logger-impl
// Injects com.Metrics with com.MetricsImpl from :impl-metrics
//
// :impl-metrics

@OptIn(ExperimentalCoroutinesApi::class)
class HotwireRepo(
    private val reportDataRepo: ReportDataRepo,
    private val requestedModules: List<ModulePath>
) {

    val code = MutableStateFlow("")
    private val requiredModules = MutableStateFlow(requestedModules.distinct())
    private val requiredInjectsByModule =
        MutableStateFlow<Map<ModulePath, List<DiProvidesAndInjectsItem.Injects>>>(mapOf())

    init {
        CoroutineScope(Dispatchers.Default).launch {
            requiredModules.collect { requiredModules ->
                val injectsForRequiredModules = reportDataRepo.diInjects(requiredModules).first().distinctBy { it.key }
                code.value = buildString {
                    requiredModules.forEach { requiredModule ->
                        appendLine("// $requiredModule (Selected Module)")
                    }
                    injectsForRequiredModules.onEach {
                        appendLine("// Injects ${it.key} as ____ from ___")
                        reportDataRepo.diProvides(it.key).first().forEach { provideOption ->
                            appendLine("// OPTION ${provideOption.implementationType} in ${provideOption.module}")
                        }
                    }

//                    """
//// :impl (Selected Module)
//// Injects com.android.Application with MyApplication from :app-provider
//// Injects com.Metrics with com.MetricsImpl from :impl-metrics
////
//// :app-provider
//// Injects com.Logger with com.LoggerImpl from :logger-impl
//// Injects com.Metrics with com.MetricsImpl from :impl-metrics
////
//// :impl-metrics
//            """.trimIndent()
                }
            }
        }


    }
}

@Composable
fun HotwireComposable(
    navRoute: HotwireNavRoute,
    reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
    navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo,
    collectRepo: CollectedDataRepo = DependencyGraph.collectedDataRepo,
) {
    val allModules: List<ModulePath>? by reportDataRepo.allModules.collectAsState(null)

    if (allModules == null) {
        BootstrapLoadingMessageWithSpinner()
        return
    }

    H2 { Text("Hotwire") }

    val ALL_MODULES_DATALIST_ID = "all_modules_datalist"

    Datalist({ id(ALL_MODULES_DATALIST_ID) }) { allModules?.sorted()?.forEach { Option(it) } }
    BootstrapSearchBox(
        query = navRoute.path,
        placeholderText = "",
        dataListId = ALL_MODULES_DATALIST_ID,
        textUpdated = {
            navRouteRepo.updateNavRoute(
                HotwireNavRoute(
                    path = it
                )
            )
        }
    )

    val diInjectsForModule by reportDataRepo.diInjects(listOfNotNull(navRoute.path)).collectAsState(null)

    if (diInjectsForModule == null) {
        BootstrapLoadingMessageWithSpinner()
        return
    }

    val gradlePath = navRoute.path
    println("gradlePath: $gradlePath")


    Hr { }

    val realCode by HotwireRepo(reportDataRepo, listOfNotNull(navRoute.path)).code.collectAsState("")
    Pre({
        classes("pre-scrollable")
    }) {
        Code {
            Text(realCode)
        }
    }


//    HighlightedCodeBlock(code, "kotlin")

    Ul {
        diInjectsForModule!!.filterIsInstance<DiProvidesAndInjectsItem.Injects>().forEach {
            Li { Text("Requires ${it.type}") }
        }
    }

}


@Composable
fun HighlightedCodeBlock(content: String, language: String) {
    Pre {
        Code({ classes(("language-$language")) }) {
            Text(content)
            SideEffect {
                highlightJsHighlightAll()
            }
        }
    }
}
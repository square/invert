package com.squareup.invert.common.pages


import PagingConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.DependencyGraph
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.common.navigation.routes.OwnerDetailNavRoute
import com.squareup.invert.models.GradlePath
import com.squareup.invert.models.OwnerName
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Text
import ui.BootstrapLoadingSpinner
import ui.BootstrapTable
import kotlin.reflect.KClass

object OwnersReportPage : InvertReportPage<OwnersReportPage.OwnersNavRoute> {
    override val navPage: NavPage = NavPage(
        pageId = "owners",
        displayName = "Owners",
        navIconSlug = "people",
        navRouteParser = { OwnersNavRoute }
    )
    override val navRouteKClass: KClass<OwnersNavRoute> = OwnersNavRoute::class

    override val composableContent: @Composable (OwnersNavRoute) -> Unit = { navRoute ->
        OwnersComposable(navRoute)
    }

    object OwnersNavRoute : BaseNavRoute(navPage)
}


@Composable
fun OwnersComposable(
    navRoute: OwnersReportPage.OwnersNavRoute,
    reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
    navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo,
) {
    H1 {
        Text("Owners")
    }

    val ownersCollected: Map<OwnerName, List<GradlePath>>? by reportDataRepo.ownerNameToModulesMap.collectAsState(null)

    if (ownersCollected == null) {
        BootstrapLoadingSpinner()
        return
    }
    val owners = ownersCollected!!

    val values: List<List<String>> = owners.map {
        listOf(it.key, it.value.size.toString())
    }

    BootstrapTable(
        headers = listOf("Owner", "Module Count"),
        rows = values,
        types = listOf(String::class, Int::class),
        maxResultsLimitConstant = PagingConstants.MAX_RESULTS
    ) { cellValues ->
        val owner = cellValues[0]
        navRouteRepo.updateNavRoute(OwnerDetailNavRoute(owner))
    }
}

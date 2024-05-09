package ui

import PagingConstants.MAX_RESULTS
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.OwnerDetailNavRoute
import com.squareup.invert.common.pages.ModuleDetailNavRoute
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Text


@Composable
fun OwnerDetailComposable(
    reportDataRepo: ReportDataRepo,
    navRouteRepo: NavRouteRepo,
    ownerDetailNavRoute: OwnerDetailNavRoute
) {
    val ownerName = ownerDetailNavRoute.owner
    val ownerToModulesMap by reportDataRepo.ownerNameToModulesMap.collectAsState(mapOf())
    val modules = ownerToModulesMap?.get(ownerName) ?: listOf()

    val ownerInfoComposable: @Composable () -> Unit = {
        H1 {
            Text("$ownerName owns ${modules.size} Modules")
        }

        BootstrapTable(
            headers = listOf("Module"),
            rows = modules.map { listOf(it) },
            types = listOf(String::class),
            maxResultsLimitConstant = MAX_RESULTS
        ) { cellValues ->
            navRouteRepo.updateNavRoute(ModuleDetailNavRoute(cellValues[0]))
        }
    }

    BootstrapTabPane(
        listOf(
            BootstrapTabData("Info", ownerInfoComposable),
        )
    )

}

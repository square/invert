package ui

import PagingConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.PluginDetailNavRoute
import com.squareup.invert.common.pages.ModuleDetailNavRoute
import org.jetbrains.compose.web.dom.Br


@Composable
fun PluginDetailComposable(reportDataRepo: ReportDataRepo, navRouteRepo: NavRouteRepo, navRoute: PluginDetailNavRoute) {
    val pluginId = navRoute.pluginId
    val pluginIdToModulesMap by reportDataRepo.pluginIdToAllModulesMap.collectAsState(mapOf())
    val modules = pluginIdToModulesMap?.get(pluginId)
    if (modules == null) {
        BootstrapLoadingSpinner()
    } else {
        TitleRow("Plugin Detail ${navRoute.pluginId} is used by ${modules.size} Modules:")
        Br()
        BootstrapClickableList("Modules Using ${pluginId}", modules, PagingConstants.MAX_RESULTS) { gradlePath ->
            navRouteRepo.updateNavRoute(ModuleDetailNavRoute(gradlePath))
        }
    }

}
package ui

import PagingConstants.MAX_RESULTS
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.PluginDetailNavRoute


@Composable
fun PluginsComposable(reportDataRepo: ReportDataRepo, navRouteRepo: NavRouteRepo) {
    val pluginIdListOrig by reportDataRepo.allPluginIds.collectAsState(null)
    pluginIdListOrig?.let { pluginIds ->
        val count = pluginIds.size
        TitleRow("Applied Plugins ($count Total)")

        BootstrapClickableList("Plugins", pluginIds, MAX_RESULTS) { gradlePluginId ->
            navRouteRepo.updateNavRoute(PluginDetailNavRoute(gradlePluginId))
        }
    } ?: BootstrapLoadingSpinner()
}
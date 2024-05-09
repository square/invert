package ui

import androidx.compose.runtime.Composable
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.ArtifactDetailNavRoute
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.Text

@Composable
fun ArtifactDetailComposable(
    reportDataRepo: ReportDataRepo,
    navRouteRepo: NavRouteRepo,
    navRoute: ArtifactDetailNavRoute
) {

    H1 { Text("Artifact Detail ${navRoute.group}:${navRoute.artifact}") }
    Br()
    H3 { Text("...") }
}

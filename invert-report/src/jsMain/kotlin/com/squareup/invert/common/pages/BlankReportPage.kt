package com.squareup.invert.common.pages


import androidx.compose.runtime.Composable
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import org.jetbrains.compose.web.dom.Text
import kotlin.reflect.KClass

class BlankNavRoute : BaseNavRoute(ArtifactsReportPage.navPage)

object BlankReportPage : InvertReportPage<ArtifactsNavRoute> {
    override val navPage: NavPage = NavPage(
        pageId = "blank",
        navRouteParser = { OwnersNavRoute }
    )
    override val navRouteKClass: KClass<ArtifactsNavRoute> = ArtifactsNavRoute::class

    override val composableContent: @Composable (ArtifactsNavRoute) -> Unit = { navRoute ->
        Text(navRoute.toString())
    }

    object OwnersNavRoute : BaseNavRoute(navPage)
}


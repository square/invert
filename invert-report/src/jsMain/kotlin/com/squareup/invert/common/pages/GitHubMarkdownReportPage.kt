package com.squareup.invert.common.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavPage.NavItem
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.common.pages.GitHubMarkdownReportPage.navPage
import com.squareup.invert.common.pages.GithubReadMeNavRoute.Companion.FILE_KEY
import com.squareup.invert.common.pages.GithubReadMeNavRoute.Companion.REPO_KEY
import highlightJsHighlightAll
import org.jetbrains.compose.web.dom.Code
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Pre
import org.jetbrains.compose.web.dom.Text
import ui.RemoteGitHubContent
import kotlin.reflect.KClass

data class GithubReadMeNavRoute(
    val orgSlashRepo: String,
    val filePath: String,
) : BaseNavRoute(navPage) {
    override fun toSearchParams(): Map<String, String> {
        val searchParams = super.toSearchParams().toMutableMap()

        searchParams[REPO_KEY] = orgSlashRepo

        searchParams[FILE_KEY] = filePath
        return searchParams
    }

    companion object {
        const val REPO_KEY = "repo"
        const val FILE_KEY = "file"
    }
}

object GitHubMarkdownReportPage : InvertReportPage<GithubReadMeNavRoute> {

    fun gitHubContentNavItem(title: String, destinationNavRoute: NavRoute): NavItem {
        return NavItem(
            navPage = GitHubMarkdownReportPage.navPage,
            itemTitle = title,
            navIconSlug = "github",
            destinationNavRoute = destinationNavRoute,
            matchesCurrentNavRoute = { it == destinationNavRoute }
        )
    }

    override val navPage: NavPage = NavPage(
        pageId = "github_readme",
        displayName = "ReadMe on GitHub",
        navIconSlug = "file-earmark-bar-graph",
        navRouteParser = {
            val orgSlashRepo = it[REPO_KEY]
            val filePath = it[FILE_KEY]
            if (orgSlashRepo != null && filePath != null) {
                GithubReadMeNavRoute(orgSlashRepo, filePath)
            } else {
                HomeReportPage.HomeNavRoute
            }
        }
    )

    override val navRouteKClass: KClass<GithubReadMeNavRoute> = GithubReadMeNavRoute::class

    override val composableContent: @Composable (GithubReadMeNavRoute) -> Unit = { navRoute ->
        H2 {
            Text("Loaded ${navRoute.filePath} from ${navRoute.orgSlashRepo}")
        }
        val url = "https://api.github.com/repos/${navRoute.orgSlashRepo}/contents/${navRoute.filePath}"
        RemoteCodeHighlighted(url)
    }
}

@Composable
fun RemoteCodeHighlighted(remoteUrl: String = "https://api.github.com/repos/square/okhttp/contents/okhttp/src/main/kotlin/okhttp3/OkHttp.kt") {
    key(remoteUrl) {
        RemoteGitHubContent(remoteUrl) { content ->
            Pre {
                val fileExtension = remoteUrl.substringAfterLast(".")
                Code({ classes(("language-$fileExtension")) }) {
                    Text(content)
                    SideEffect {
                        highlightJsHighlightAll()
                    }
                }
            }
        }
    }
}
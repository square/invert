import androidx.compose.runtime.Composable
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.common.pages.HomeReportPage
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Text
import ui.RemoteGitHubMarkdown
import kotlin.reflect.KClass

object GitHubMarkdownReportPage : InvertReportPage<GitHubMarkdownReportPage.GithubReadMeNavRoute> {
    override val navPage: NavPage = NavPage(
        pageId = "github_readme",
        displayName = "ReadMe on GitHub",
        navIconSlug = "file-earmark-bar-graph",
        navRouteParser = {
            val repo = it[REPO_KEY]
            if (repo != null) {
                GithubReadMeNavRoute(repo)
            } else {
                HomeReportPage.HomeNavRoute
            }
        }
    )
    override val navRouteKClass: KClass<GithubReadMeNavRoute> = GithubReadMeNavRoute::class

    override val composableContent: @Composable (GithubReadMeNavRoute) -> Unit = { navRoute ->
        println("RENDER ${navRoute.orgSlashRepo}")
        // Your Composable content here
        H2 {
            Text("README for ${navRoute.orgSlashRepo}")
        }
        val url = "https://api.github.com/repos/${navRoute.orgSlashRepo}/contents/README.md"
        RemoteGitHubMarkdown(url)
    }

    private const val REPO_KEY = "repo"


    data class GithubReadMeNavRoute(val orgSlashRepo: String? = null) : BaseNavRoute(navPage) {
        override fun toSearchParams(): Map<String, String> {
            val searchParams = super.toSearchParams().toMutableMap()
            orgSlashRepo?.let {
                searchParams[REPO_KEY] = it
            }
            return searchParams
        }

        companion object {
            val REPO_KEY = "repo"
        }
    }
}

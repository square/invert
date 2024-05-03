import com.squareup.af.analysis.navigation.routes.GithubReadMeNavRoute
import com.squareup.invert.common.InvertReport
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.routes.DependencyDiffNavRoute
import com.squareup.invert.common.navigation.routes.ModuleConsumptionNavRoute
import navigation.CustomNavItem
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Text
import ui.RemoteGitHubMarkdown

fun main() {
    val customNavItems = listOf(
        CustomNavItem(
            text = "What Demo Apps?",
            iconSlug = "question-circle",
            navRoute = ModuleConsumptionNavRoute(
                pluginGroupByFilter = listOf(
                    "com.squareup.gradle.DemoAppPlugin"
                ),
                configurations = listOf("debugRuntimeClasspath"),
                moduleQuery = ":features:checkout-v2:"
            ),
        ),
        CustomNavItem(
            text = "Dependency Diff :invert-models w/js & jvm",
            iconSlug = "question-circle",
            navRoute = DependencyDiffNavRoute(
                moduleA = ":invert-models",
                moduleB = ":invert-models",
                configurationA = "jvmRuntimeClasspath",
                configurationB = "jsRuntimeClasspath",
                includeArtifacts = true,
                showMatching = false,
            ),
        ),
        CustomNavItem(
            text = "Anvil GitHub README",
            iconSlug = "bar-chart",
            navRoute = GithubReadMeNavRoute("square/anvil"),
        ),
        CustomNavItem(
            text = "Papa GitHub README",
            iconSlug = "bar-chart",
            navRoute = GithubReadMeNavRoute("square/papa"),
        ),
    )

    val reportPages = listOf(
        InvertReportPage(
            GithubReadMeNavRoute().navPage,
            GithubReadMeNavRoute::class
        ) {
            val navRoute = it as GithubReadMeNavRoute
            // Your Composable content here
            H2 {
                Text("README for ${navRoute.orgSlashRepo}")
            }
            RemoteGitHubMarkdown("https://api.github.com/repos/${navRoute.orgSlashRepo}/contents/README.md")
        }
    )

    InvertReport(
        customNavItems = customNavItems,
        reportPages = reportPages,
    )
}

import com.squareup.invert.common.InvertReport
import com.squareup.invert.common.navigation.routes.DependencyDiffNavRoute
import com.squareup.invert.common.navigation.routes.ModuleConsumptionNavRoute
import com.squareup.invert.common.pages.DependencyDiffNavRoute
import navigation.CustomNavItem

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
            navRoute = GitHubMarkdownReportPage.GithubReadMeNavRoute("square/anvil"),
        ),
        CustomNavItem(
            text = "Papa GitHub README",
            iconSlug = "bar-chart",
            navRoute = GitHubMarkdownReportPage.GithubReadMeNavRoute("square/papa"),
        ),
    )

    InvertReport(
        customNavItems = customNavItems,
        customReportPages = listOf(
            GitHubMarkdownReportPage
        ),
    )
}

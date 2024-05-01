import com.squareup.af.analysis.navigation.routes.InvertOnGithubNavRoute
import com.squareup.invert.common.InvertReport
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.routes.DependencyDiffNavRoute
import com.squareup.invert.common.navigation.routes.ModuleConsumptionNavRoute
import navigation.CustomNavPage
import ui.AndroidModuleMetricsDashboard
import ui.FullScreenIframe

fun main() {

    val customNavPages = listOf(
        CustomNavPage(
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
        CustomNavPage(
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
        CustomNavPage(
            text = "Invert on GitHub",
            iconSlug = "bar-chart",
            navRoute = InvertOnGithubNavRoute,
        ),
    )

    InvertReport(
        customNavItems = customNavPages,
        customComposables = mapOf(
            InvertOnGithubNavRoute::class to {
                // Your Composable content here
                FullScreenIframe("https://github.com/squareup/invert")
            }
        ),
        customPages = listOf<NavPage>(
            InvertOnGithubNavRoute.navPage,
        )
    )
}

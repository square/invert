package navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.common.navigation.NavRouteRepo
import org.jetbrains.compose.web.dom.*
import ui.BootstrapIcon
import ui.BootstrapNavItem
import ui.BootstrapNavSectionHeader
import kotlin.random.Random

data class CustomNavItem(
    val text: String,
    val iconSlug: String,
    val navRoute: NavRoute
)

@Composable
fun LeftNavigationComposable(initialRoute: NavRoute, navRouteRepo: NavRouteRepo, customReports: List<CustomNavItem>) {
    val currentNavRoute by navRouteRepo.navRoute.collectAsState(initialRoute)

    Ul({ classes("list-unstyled", "ps-0") }) {
        val random = Random(0)
        NavPage
            .ROOT_NAV_ITEMS
            .forEach { navPageGroup: NavPage.NavPageGroup ->

                val collapseId = "nav-group-id-" + random.nextInt()
                Li({ classes("mb-1") }) {
                    Button({
                        classes(
                            "btn btn-toggle d-inline-flex align-items-center rounded border-0 collapsed".split(" ")
                        )
                        attr("data-bs-toggle", "collapse")
                        attr("data-bs-target", "#$collapseId")
                        attr("aria-expanded", "true")
                    }) {
                        Text(navPageGroup.groupTitle)
                    }
                    Div({
                        classes("collapse", "show")
                        id(collapseId)
                    }) {
                        Ul({ classes("btn-toggle-nav list-unstyled fw-normal pb-1 small".split(" ")) }) {
                            navPageGroup.navItems.forEach { rootNavItem ->
                                val newNavRoute = rootNavItem.navRouteParser(mapOf())
                                val activeTab = currentNavRoute::class == newNavRoute::class
                                Li {
                                    A(
                                        href = "#",
                                        {
                                            classes(
                                                "nav-link link-body-emphasis d-inline-flex text-decoration-none rounded".split(
                                                    " "
                                                )
                                                    .toMutableList().also {
                                                        if (activeTab) {
                                                            it.add("active")
                                                        }
                                                    })
                                            onClick { navRouteRepo.updateNavRoute(newNavRoute) }
                                        }) {

                                        BootstrapIcon(rootNavItem.navIconSlug) {}
                                        Span({
                                            classes("ps-2")
                                        }) {
                                            Text(rootNavItem.displayName)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }
    return
}

@Composable
fun OldNav(currentNavRoute: NavRoute, navRouteRepo: NavRouteRepo, customReports: List<CustomNavItem>) {
    Hr { }

    /** Entry Point */
    NavPage
        .ROOT_NAV_ITEMS.map { it.navItems }.flatten()
        .forEach { rootNavItem: NavPage ->
            val displayName = rootNavItem.displayName
            val navIconSlug = rootNavItem.navIconSlug
            val newNavRoute = rootNavItem.navRouteParser(mapOf())
            BootstrapNavItem(
                text = displayName,
                iconSlug = navIconSlug,
                activeTab = currentNavRoute::class == newNavRoute::class,
                onClick = {
                    navRouteRepo.updateNavRoute(newNavRoute)
                }
            )
        }

    if (customReports.isNotEmpty()) {
        BootstrapNavSectionHeader("Custom Reports", "fire")

        customReports.forEach {
            BootstrapNavItem(
                text = it.text,
                iconSlug = it.iconSlug,
                onClick = { navRouteRepo.updateNavRoute(it.navRoute) }
            )
        }
    }
}

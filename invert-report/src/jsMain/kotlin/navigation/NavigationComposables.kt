package navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.common.navigation.NavRouteRepo
import org.jetbrains.compose.web.attributes.ATarget.Blank
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.dom.*
import ui.BootstrapIcon
import kotlin.random.Random

data class CustomNavItem(
    val text: String,
    val iconSlug: String,
    val navRoute: NavRoute
)

@Composable
fun LeftNavigationComposable(
    initialRoute: NavRoute,
    navRouteRepo: NavRouteRepo,
    reportDataRepo: ReportDataRepo,
) {
    val currentNavRoute by navRouteRepo.navRoute.collectAsState(initialRoute)
    val metadataOrig by reportDataRepo.reportMetadata.collectAsState(null)

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
                            navPageGroup.navItems.forEach { rootNavItem: NavPage.NavItem ->
                                val activeTab = rootNavItem.matchesCurrentNavRoute(currentNavRoute)
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
                                            onClick { navRouteRepo.updateNavRoute(rootNavItem.destinationNavRoute) }
                                        }) {

                                        BootstrapIcon(rootNavItem.navIconSlug) {}
                                        Span({
                                            classes("ps-2")
                                        }) {
                                            Text(rootNavItem.itemTitle)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }

    metadataOrig?.let { metadata ->
        Small({classes("text-center")}) {
            Br()
            metadata.branchName?.let { branchName ->
                Text("Branch ")
                A(href = metadata.remoteRepoUrl + "/tree/${branchName}", attrs = { target(Blank) }) {
                    Text(branchName)
                }
            }
            Br()
            metadata.currentBranchHash?.let { currentBranchHash ->
                Text("Commit ")
                A(href = metadata.remoteRepoUrl + "/tree/${currentBranchHash}", attrs = { target(Blank) }) {
                    Text(currentBranchHash.substring(0, 7))
                }
            }
            Br()
            Text("${metadata.timeStr}")
            Br()
            Text("(${metadata.timezoneId})")
            Br()
            Br()
            Br()
            Br()
        }
    }
}

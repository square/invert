package navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.DefaultNavItems
import com.squareup.invert.common.navigation.NavGroupsRepo
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavPage.NavItem
import com.squareup.invert.common.navigation.NavPageGroup
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.pages.AllStatsReportPage
import com.squareup.invert.common.pages.StatDetailNavRoute
import com.squareup.invert.models.StatMetadata
import com.squareup.invert.models.js.CollectedStatTotalsJsReportModel
import org.jetbrains.compose.web.attributes.ATarget.Blank
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Li
import org.jetbrains.compose.web.dom.Small
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Ul
import ui.BootstrapIcon
import kotlin.random.Random

@Composable
fun LeftNavigationComposable(
  initialRoute: NavRoute,
  navRouteRepo: NavRouteRepo,
  reportDataRepo: ReportDataRepo,
  navGroupsRepo: NavGroupsRepo,
) {
  val currentNavRoute by navRouteRepo.navRoute.collectAsState(initialRoute)
  val metadataOrig by reportDataRepo.reportMetadata.collectAsState(null)
  val statTotals: CollectedStatTotalsJsReportModel? by reportDataRepo.statTotals.collectAsState(null)

  val otherNavGroups: List<NavPageGroup> = if (statTotals != null) {
    statTotals!!.statTotals.entries
      .groupBy { a: Map.Entry<StatMetadata, Int> -> a.key.category }
      .mapNotNull { categoryToEntries: Map.Entry<String?, List<Map.Entry<StatMetadata, Int>>> ->
        if (categoryToEntries.key == null) {
          null
        } else {
          val groupTitle = categoryToEntries.key!!.replace("_", " ")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
          val navItems = categoryToEntries.value.map {
            NavItem(
              itemTitle = it.key.description,
              navPage = AllStatsReportPage.navPage,
              destinationNavRoute = StatDetailNavRoute(
                pluginIds = listOf(),
                statKeys = listOf(it.key.key)
              ),
              matchesCurrentNavRoute = { false },
              navIconSlug = "record"
            )
          }.toSet()
          NavPageGroup(
            groupTitle = groupTitle,
            navItems = navItems
          )
        }
      }
  } else {
    listOf()
  }

  Ul({ classes("list-unstyled", "ps-0") }) {
    val random = Random(0)
    val navGroups by navGroupsRepo.navGroups.collectAsState(setOf())
    navGroups.plus(otherNavGroups)
      .forEach { navPageGroup: NavPageGroup ->
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
    Small({ classes("text-center") }) {
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

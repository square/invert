package navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavGroupsRepo
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavPageGroup
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.toQueryString
import com.squareup.invert.common.utils.FormattingUtils.dateDisplayStr
import history.PushOrReplaceState
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
import ui.BootstrapLoadingSpinner
import kotlin.random.Random

@Composable
fun LeftNavigationComposable(
  initialRoute: NavRoute,
  navRouteRepo: NavRouteRepo,
  reportDataRepo: ReportDataRepo,
  navGroupsRepo: NavGroupsRepo,
) {
  val currentNavRoute by navRouteRepo.navRoute.collectAsState(
    NavRouteRepo.NavChangeEvent(
      initialRoute,
      PushOrReplaceState.PUSH
    )
  )
  val metadataOrig by reportDataRepo.reportMetadata.collectAsState(null)

  if (metadataOrig == null) {
    //Loading, don't show anymore for the moment.
    BootstrapLoadingSpinner()
    return
  }
  val metadata = metadataOrig!!
  Ul({ classes("list-unstyled", "ps-0") }) {
    val random = Random(0)
    val navGroups by navGroupsRepo.navGroups(metadata.buildSystem).collectAsState(setOf())
    navGroups.forEach { navPageGroup: NavPageGroup ->
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
              val activeTab = rootNavItem.matchesCurrentNavRoute(currentNavRoute.navRoute)
              Li {
                A(
                  href = rootNavItem.destinationNavRoute.toQueryString(),
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
                    onClick { event ->
                      if (event.ctrlKey || event.metaKey) {
                        // If it's a ctrl-click, allow user to open in new tab
                        return@onClick
                      }

                      event.preventDefault()
                      navRouteRepo.pushNavRoute(rootNavItem.destinationNavRoute)
                    }
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

  Small({ classes("text-center") }) {
    Br()
    metadata.branchName?.let { branchName ->
      Text("Branch ")
      A(href = metadata.remoteRepoUrl + "/tree/${branchName}", attrs = { target(Blank) }) {
        Text(branchName)
      }
    }
    Br()
    metadata.latestCommitGitSha.let { currentBranchHash ->
      Text("Commit ")
      A(href = metadata.remoteRepoUrl + "/tree/${currentBranchHash}", attrs = { target(Blank) }) {
        Text(currentBranchHash.substring(0, 7))
      }
    }
    Br()
    Text(metadata.dateDisplayStr())
    Br()
    Br()
    Br()
    Br()
  }
}

package ui

import PagingConstants.MAX_RESULTS
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.ModuleDetailNavRoute
import com.squareup.invert.common.navigation.routes.OwnerDetailNavRoute
import com.squareup.invert.models.OwnerName
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Text


@Composable
fun ModuleDetailComposable(
  reportDataRepo: ReportDataRepo,
  navRouteRepo: NavRouteRepo,
  navRoute: ModuleDetailNavRoute
) {
  val modulePath = navRoute.path

  val directDependenciesMapOrig by reportDataRepo.directDependenciesOf(modulePath).collectAsState(null)
  val moduleToOwnerMapCollected by reportDataRepo.moduleToOwnerMap.collectAsState(null)

  if (moduleToOwnerMapCollected == null) {
    H1 {
      BootstrapLoadingMessageWithSpinner("Loading Module Ownership...")
    }
    return
  }
  val moduleToOwnerMap = moduleToOwnerMapCollected!!
  val ownerName: OwnerName? = moduleToOwnerMap[modulePath]
  ownerName?.let {
    H1 {
      Text("Module $modulePath is owned by ")
      AppLink({
        onClick {
          navRouteRepo.updateNavRoute(OwnerDetailNavRoute(ownerName))
        }
      }) {
        Text(ownerName)
      }
    }
  }
  val tabs = mutableListOf<BootstrapTabData>()

  if (directDependenciesMapOrig == null) {
    BootstrapLoadingMessageWithSpinner("Loading Direct Dependencies...")
    return
  }
  directDependenciesMapOrig?.keys?.forEach { configurationName ->
    tabs.add(
      BootstrapTabData("Direct Dependencies for $configurationName") {
        val rows = directDependenciesMapOrig?.get(configurationName)
          ?.filter { it.startsWith(":") }
          ?.sorted() ?: listOf()
        BootstrapTable(
          headers = listOf("Module"),
          rows = rows.map { listOf(it) },
          types = listOf(String::class),
          maxResultsLimitConstant = MAX_RESULTS,
          onItemClick = {
            navRouteRepo.updateNavRoute(ModuleDetailNavRoute(it[0]))
          }
        )
      }
    )
  }


  val configurationToDependencyMapCollected by reportDataRepo.dependenciesOf(navRoute.path).collectAsState(null)
  if (configurationToDependencyMapCollected == null) {
    H1 {
      BootstrapLoadingMessageWithSpinner("Loading Transitive Dependencies...")
    }
    return
  }

  val configurationToDependencyMap = configurationToDependencyMapCollected!!
  configurationToDependencyMap.keys.forEach { configurationName ->
    tabs.add(
      BootstrapTabData("Transitive Dependencies for $configurationName") {
        val rows = configurationToDependencyMap[configurationName]
          ?.filter { it.startsWith(":") }
          ?.sorted() ?: listOf()
        BootstrapTable(
          headers = listOf("Module"),
          rows = rows.map { listOf(it) },
          types = listOf(String::class),
          maxResultsLimitConstant = MAX_RESULTS,
          onItemClick = {
            navRouteRepo.updateNavRoute(ModuleDetailNavRoute(it[0]))
          }
        )
      }
    )
  }
  tabs.add(BootstrapTabData("Module Used By...") {
    val moduleUsageCollected by reportDataRepo.moduleUsedBy(modulePath).collectAsState(null)
    if (moduleUsageCollected == null) {
      H1 {
        BootstrapLoadingMessageWithSpinner("Loading Module Usage...")
      }
      return@BootstrapTabData
    }
    val moduleUsage = moduleUsageCollected!!
    BootstrapTable(
      headers = listOf("Module", "Used in Configurations"),
      rows = moduleUsage.keys.map { key -> listOf(key, moduleUsage[key].toString()) },
      types = moduleUsage.keys.map { String::class },
      maxResultsLimitConstant = MAX_RESULTS,
      onItemClick = {
        navRouteRepo.updateNavRoute(
          ModuleDetailNavRoute(
            it[0]
          )
        )
      }
    )
  })
  BootstrapTabPane(tabs)
}
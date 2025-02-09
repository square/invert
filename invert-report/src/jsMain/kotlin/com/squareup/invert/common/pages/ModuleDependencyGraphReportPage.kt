package com.squareup.invert.common.pages


import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.DependencyGraph
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.common.pages.ModuleDependencyGraphNavRoute.Companion.parser
import com.squareup.invert.models.ConfigurationName
import com.squareup.invert.models.DependencyId
import com.squareup.invert.models.InvertSerialization
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.web.attributes.list
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.border
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.style
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Datalist
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Fieldset
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Legend
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextInput
import render3dGraph
import ui.BootstrapColumn
import ui.BootstrapLoadingMessageWithSpinner
import ui.BootstrapRow
import kotlin.random.Random
import kotlin.reflect.KClass

data class ModuleDependencyGraphNavRoute(
  val module: String? = null,
  val configuration: String? = null,
) : BaseNavRoute(ModuleDependencyGraphReportPage.navPage) {

  override fun toSearchParams(): Map<String, String> = toParamsWithOnlyPageId(this)
    .also { params ->
      module?.let {
        params[MODULE_PARAM] = module
      }
      configuration?.let {
        params[CONFIGURATION_PARAM] = configuration
      }
    }

  companion object {

    private const val MODULE_PARAM = "module"
    private const val CONFIGURATION_PARAM = "configuration"

    fun parser(params: Map<String, String?>): ModuleDependencyGraphNavRoute {
      return ModuleDependencyGraphNavRoute(
        module = params[MODULE_PARAM],
        configuration = params[CONFIGURATION_PARAM]
      )
    }
  }
}


object ModuleDependencyGraphReportPage : InvertReportPage<ModuleDependencyGraphNavRoute> {
  override val navPage: NavPage = NavPage(
    pageId = "module_dependency_graph",
    displayName = "Module Dependency Graph",
    navIconSlug = "diagram-3",
    navRouteParser = { parser(it) }
  )

  override val navRouteKClass: KClass<ModuleDependencyGraphNavRoute> = ModuleDependencyGraphNavRoute::class

  override val composableContent: @Composable (ModuleDependencyGraphNavRoute) -> Unit = { navRoute ->
    ModuleDependencyGraphComposable(navRoute)
  }
}


@Serializable
data class GraphNode(
  val id: String,
  val group: Int
)

@Serializable
data class GraphLink(
  val source: String,
  val target: String,
  val group: Int
)

@Serializable
data class GraphData(
  val nodes: MutableList<GraphNode> = mutableListOf(),
  val links: MutableList<GraphLink> = mutableListOf(),
)

@Composable
fun ModuleDependencyGraphComposable(
  dependencyGraphNavRoute: ModuleDependencyGraphNavRoute,
  reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
  navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo,
) {
  val rootModulePath = dependencyGraphNavRoute.module ?: ":api:sales-report:impl"
  val allDirectDeps by reportDataRepo.allDirectDependencies.collectAsState(null)
  val allModules by reportDataRepo.allModules.collectAsState(null)
  val allAnalyzedConfigurationNames by reportDataRepo.allAnalyzedConfigurationNames.collectAsState(null)

  if (allDirectDeps == null || allAnalyzedConfigurationNames == null) {
    BootstrapLoadingMessageWithSpinner()
    return
  }

  val DEPENDENCY_GRAPH_MODULES_DATALIST_ID = "dependency_graph_modules_datalist"
  Datalist({ id(DEPENDENCY_GRAPH_MODULES_DATALIST_ID) }) {
    allModules?.map { Option(it) }
  }
  val ANALYZED_CONFIGURATION_NAMES_DATALIST_ID = "analyzed_configuration_names_datalist"
  Datalist({ id(ANALYZED_CONFIGURATION_NAMES_DATALIST_ID) }) {
    allAnalyzedConfigurationNames?.map { Option(it) }
  }

  if (allAnalyzedConfigurationNames!!.isEmpty()) {
    H1 { Text("No analyzed configurations found.") }
    return
  }

  val selectedConfiguration = dependencyGraphNavRoute.configuration ?: allAnalyzedConfigurationNames!!.first()

  BootstrapRow {
    BootstrapColumn(12) {
      Fieldset {
        Legend { Text("Module Dependency Graph") }
        P {
          Label { Text("Module") }
          TextInput(dependencyGraphNavRoute.module ?: "") {
            classes("form-control")
            list(DEPENDENCY_GRAPH_MODULES_DATALIST_ID)
            placeholder("Module...")
            onInput { navRouteRepo.pushNavRoute(dependencyGraphNavRoute.copy(module = it.value)) }
          }
          TextInput(selectedConfiguration) {
            classes("form-control")
            list(ANALYZED_CONFIGURATION_NAMES_DATALIST_ID)
            placeholder("Configuration...")
            onInput { navRouteRepo.pushNavRoute(dependencyGraphNavRoute.copy(configuration = it.value)) }
          }
        }
      }
    }
  }

  val directDepsForModuleMap = allDirectDeps?.get(rootModulePath)


  val graphDomId = "dependency-force-graph"
  val width = 1200
  val height = 768

  fun pullModulesOnDebugRuntimeClasspathDeps(direct: Map<ConfigurationName, Set<DependencyId>>?): List<DependencyId> {
    return direct
      ?.filterKeys { it == selectedConfiguration }
      ?.values
      ?.flatten()
      ?.filter { it.startsWith(":") }
      ?: listOf()
  }

  val directDepsForModuleListFiltered = pullModulesOnDebugRuntimeClasspathDeps(directDepsForModuleMap)

  println(dependencyGraphNavRoute.module)
  println(allModules)
  val isValidModule = allModules?.contains(dependencyGraphNavRoute.module) ?: false
  if (isValidModule) {

    if (directDepsForModuleListFiltered.isEmpty()) {
      H1 { Text("${dependencyGraphNavRoute.module} has 0 direct dependencies on the $selectedConfiguration") }
      return
    }

    // Good to go
    Div({
      id(graphDomId)
      style {
        border {
          width(2.px)
          style(LineStyle.Solid)
          color = Color.black
        }
        width(width.px)
        height(height.px)
      }
    })
  } else {
    H1 { Text("Enter a valid module.") }
    return
  }

  // https://github.com/vasturiano/force-graph
  val forceGraphJsonData = GraphData()

  fun findIdx(dependencyId: DependencyId): Int {
    val idx = forceGraphJsonData.nodes.indexOfFirst { dependencyId == it.id }
    return if (idx == -1) {
      Random.nextInt()
    } else {
      idx
    }
  }

  val maxDepth = 5

  fun addMoreNodes(currDepth: Int, graphData: GraphData, modulePath: String, parentModulePath: String, groupId: Int) {
    pullModulesOnDebugRuntimeClasspathDeps(allDirectDeps?.get(modulePath)).forEachIndexed { idx, depId ->
      if (graphData.nodes.firstOrNull { it.id == depId } == null) {
        graphData.nodes.add(GraphNode(depId, findIdx(depId)))
        if (currDepth < maxDepth) {
          addMoreNodes(currDepth + 1, graphData, depId, modulePath, idx)
        }
      }
      val newLink = GraphLink(depId, parentModulePath, groupId)
      if (!graphData.links.contains(newLink)) {
        graphData.links.add(newLink)
      }
    }
  }

  forceGraphJsonData.nodes.add(GraphNode(rootModulePath, findIdx(rootModulePath)))

  directDepsForModuleListFiltered.forEach { dependencyId ->
    forceGraphJsonData.nodes.add(GraphNode(dependencyId, findIdx(dependencyId)))
  }

  directDepsForModuleListFiltered.forEach {
    addMoreNodes(1, forceGraphJsonData, it, rootModulePath, findIdx(rootModulePath))
  }

  forceGraphJsonData.links.addAll(directDepsForModuleListFiltered.map {
    GraphLink(it, rootModulePath, findIdx(rootModulePath))
  })

  CoroutineScope(Dispatchers.Main).launch {
    delay(200)
    render3dGraph(
      graphDomId,
      InvertSerialization.InvertJson.encodeToString(GraphData.serializer(), forceGraphJsonData),
      width,
      height
    )
  }
}


package com.squareup.invert.common.pages


import PagingConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.DependencyGraph
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.httpsUrlForCommit
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.common.pages.StatDetailNavRoute.Companion.parser
import com.squareup.invert.models.CollectedStatType
import com.squareup.invert.models.GradlePluginId
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatKey
import com.squareup.invert.models.StatMetadata
import com.squareup.invert.models.js.MetadataJsReportModel
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.Text
import ui.BootstrapLoadingMessageWithSpinner
import ui.BootstrapLoadingSpinner
import ui.BootstrapSearchBox
import ui.BootstrapSettingsCheckbox
import ui.BootstrapTabData
import ui.BootstrapTabPane
import ui.BootstrapTable
import ui.MarkdownCellContent
import kotlin.reflect.KClass

data class StatDetailNavRoute(
  val pluginIds: List<GradlePluginId> = emptyList(),
  val statKeys: List<String>,
  val moduleQuery: String? = null
) : BaseNavRoute(StatDetailReportPage.navPage) {

  override fun toSearchParams(): Map<String, String> = toParamsWithOnlyPageId(this)
    .also { params ->
      params[PLUGIN_IDS_PARAM] = pluginIds.joinToString(separator = ",")
      params[STATKEYS_PARAM] = statKeys.joinToString(separator = ",")
      moduleQuery?.let {
        params[MODULE_QUERY_PARAM] = it
      }
    }

  companion object {

    private const val PLUGIN_IDS_PARAM = "plugins"
    private const val STATKEYS_PARAM = "statkeys"
    private const val MODULE_QUERY_PARAM = "modulequery"

    fun parser(params: Map<String, String?>): StatDetailNavRoute {
      val pluginIds = params[PLUGIN_IDS_PARAM]?.split(",")?.filter { it.isNotBlank() } ?: listOf()
      val statKeys = params[STATKEYS_PARAM]?.split(",")?.filter { it.isNotBlank() } ?: listOf()
      val moduleQuery = params[MODULE_QUERY_PARAM]
      return StatDetailNavRoute(
        pluginIds = pluginIds,
        statKeys = statKeys,
        moduleQuery = moduleQuery
      )
    }
  }
}

fun Collection<Stat.CodeReferencesStat.CodeReference>.toMarkdown(projectMetadata: MetadataJsReportModel): String {
  val codeReferences = this
  return buildString {
    codeReferences.forEach { codeReference ->
      val urlToFile =
        "${projectMetadata.httpsUrlForCommit()}/${codeReference.filePath}#L${codeReference.startLine}-L${codeReference.endLine}"
      appendLine("* <a href='$urlToFile' target='_blank'>${codeReference.filePath}:${codeReference.startLine}</a>")
    }
  }
}

object StatDetailReportPage : InvertReportPage<StatDetailNavRoute> {
  override val navPage: NavPage = NavPage(
    pageId = "stat_detail",
    navRouteParser = { parser(it) }
  )

  override val navRouteKClass: KClass<StatDetailNavRoute> = StatDetailNavRoute::class

  override val composableContent: @Composable (StatDetailNavRoute) -> Unit = { navRoute ->
    StatDetailComposable(navRoute)
  }
}

@Composable
fun StatDetailComposable(
  statsNavRoute: StatDetailNavRoute,
  reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
  navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo,
) {
  val allPluginIds by reportDataRepo.allPluginIds.collectAsState(null)
  val statsData by reportDataRepo.statsData.collectAsState(null)
  val allModulesOrig by reportDataRepo.allModules.collectAsState(null)
  val moduleToOwnerMapFlowValue by reportDataRepo.moduleToOwnerMap.collectAsState(null)

  val metadata by reportDataRepo.reportMetadata.collectAsState(null)

  val statKeys = statsNavRoute.statKeys.ifEmpty {
    statsData?.statInfos?.map { it.key } ?: listOf()
  }
  H1 { Text("Stats") }

  if (moduleToOwnerMapFlowValue == null || metadata == null) {
    BootstrapLoadingSpinner()
    return
  }

  val query = statsNavRoute.moduleQuery


  val filterTab = BootstrapTabData("Filters") {
    H3 { Text("All Plugin Types") }
    allPluginIds?.forEach { pluginId ->
      BootstrapSettingsCheckbox(
        labelText = pluginId,
        initialIsChecked = statsNavRoute.pluginIds.contains(pluginId),
      ) { checked ->
        navRouteRepo.updateNavRoute(
          navRoute = statsNavRoute.copy(
            pluginIds = if (checked) {
              statsNavRoute.pluginIds.plus(pluginId)
            } else {
              statsNavRoute.pluginIds.minus(pluginId)
            }
          )
        )
      }
    }
  }

  if (allModulesOrig == null) {
    return
  }
  val allModules1 = allModulesOrig!!

  val allModules = if (query != null && query != ":" && query.isNotEmpty()) {
    allModules1.filter { it.contains(query) }
  } else {
    allModules1
  }

  BootstrapSearchBox(
    query = query ?: "",
    placeholderText = "Module Query...",
  ) {
    navRouteRepo.updateNavRoute(statsNavRoute.copy(moduleQuery = it))
  }

  val SUPPORTED_TYPES = listOf(
    CollectedStatType.STRING,
    CollectedStatType.BOOLEAN,
    CollectedStatType.NUMERIC,
    CollectedStatType.CODE_REFERENCES
  )
  val resultsTab = BootstrapTabData("Results") {
    val statsColumns = mutableListOf<List<String>>().apply {
      statKeys.forEach { statKey ->
        val statInfo = statsData?.statInfos?.get(statKey)
        statInfo?.let { statMetadata: StatMetadata ->
          if (SUPPORTED_TYPES.contains(statMetadata.statType)) {
            add(
              allModules.map { gradlePath ->
                val statsDataForModule: Map<StatKey, Stat>? = statsData?.statsByModule?.get(gradlePath)
                val stat = statsDataForModule?.get(statKey)
                when (stat) {
                  is Stat.BooleanStat -> {
                    stat.value.toString()
                  }

                  is Stat.StringStat -> {
                    stat.value
                  }

                  is Stat.NumericStat -> {
                    stat.value.toString()
                  }

                  is Stat.CodeReferencesStat -> {
                    if (metadata != null) {
                      stat.value.toMarkdown(metadata!!)
                    } else {
                      stat.value.toString()
                    }
                  }

                  else -> ""
                }
              }
            )
            add(
              allModules.map { gradlePath ->
                val statsDataForModule: Map<StatKey, Stat>? = statsData?.statsByModule?.get(gradlePath)
                val stat = statsDataForModule?.get(statKey)
                when (stat) {
                  is Stat.BooleanStat -> {
                    stat.details
                  }

                  is Stat.StringStat -> {
                    stat.details
                  }

                  is Stat.NumericStat -> {
                    stat.details
                  }

                  else -> ""
                } ?: ""
              }
            )
          }
        }
      }
    }

    val headers = mutableListOf("Module")
      .apply {
        statKeys.forEach {
          val thisStatType = statsData?.statInfos?.get(it)
          if (SUPPORTED_TYPES.contains(thisStatType?.statType)) {
            val description = statsData?.statInfos?.get(it)?.description ?: it
            add(description)
            add("$description Details")
          }
        }
      }
    val values: List<List<String>> = allModules.mapIndexed { idx, modulePath ->
      mutableListOf(
        allModules[idx]
      ).apply {
        statsColumns.forEach {
          add(it[idx])
        }
      }
    }.filter {
      var hasValue = false
      it.forEachIndexed { idx, str ->
        if (idx > 0 && str.isNotEmpty()) {
          hasValue = true
        }
      }
      hasValue
    }

    if (statsData == null) {
      BootstrapLoadingMessageWithSpinner("Loading...")
    } else {
      if (values.isNotEmpty()) {
        BootstrapTable(
          headers = headers,
          rows = values,
          types = headers.map { MarkdownCellContent::class },
          maxResultsLimitConstant = PagingConstants.MAX_RESULTS
        ) { cellValues ->
          navRouteRepo.updateNavRoute(ModuleDetailNavRoute(cellValues[0]))
        }
      } else {
        H3 { Text("No Collected Stats of Type(s) ${statsNavRoute.statKeys}") }
      }
    }
  }

  BootstrapTabPane(
    listOf(
      resultsTab,
      filterTab,
    )
  )

}
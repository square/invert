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
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.common.pages.StatDetailNavRoute.Companion.parser
import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.OwnerName
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatDataType
import com.squareup.invert.models.StatKey
import com.squareup.invert.models.js.MetadataJsReportModel
import com.squareup.invert.models.js.StatJsReportModel
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.Text
import ui.BootstrapLoadingMessageWithSpinner
import ui.BootstrapLoadingSpinner
import ui.BootstrapSearchBox
import ui.BootstrapTabData
import ui.BootstrapTabPane
import ui.BootstrapTable
import ui.MarkdownCellContent
import kotlin.reflect.KClass

data class StatDetailNavRoute(
  val statKey: StatKey,
  val moduleQuery: String? = null
) : BaseNavRoute(StatDetailReportPage.navPage) {

  override fun toSearchParams(): Map<String, String> = toParamsWithOnlyPageId(this)
    .also { params ->
      params[STATKEY_PARAM] = statKey
      moduleQuery?.let {
        params[MODULE_QUERY_PARAM] = it
      }
    }

  companion object {

    private const val STATKEY_PARAM = "stat_key"
    private const val MODULE_QUERY_PARAM = "module_query"

    fun parser(params: Map<String, String?>): NavRoute {
      val statKey = params[STATKEY_PARAM] ?: run {
        return AllStatsNavRoute()
      }
      val moduleQuery = params[MODULE_QUERY_PARAM]
      return StatDetailNavRoute(
        statKey = statKey,
        moduleQuery = moduleQuery
      )
    }
  }
}

fun Collection<Stat.CodeReferencesStat.CodeReference>.toMarkdown(projectMetadata: MetadataJsReportModel): String {
  val codeReferences = this
  return buildString {
    codeReferences.forEach { codeReference ->
      appendLine("* ${codeReference.toHrefLink(projectMetadata)}")
    }
  }
}

fun Stat.CodeReferencesStat.CodeReference.toHrefLink(
  projectMetadata: MetadataJsReportModel,
  openInNewTab: Boolean = true
): String {
  val codeReference = this
  return buildString {
    val urlToFile =
      "${projectMetadata.httpsUrlForCommit()}/${codeReference.filePath}#L${codeReference.startLine}-L${codeReference.endLine}"
    if (openInNewTab) {
      appendLine("<a href='$urlToFile' target='_blank'>${codeReference.filePath}:${codeReference.startLine}</a>")
    } else {
      appendLine("[${codeReference.filePath}:${codeReference.startLine}]($urlToFile)")
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
  val moduleToOwnerMapFlowValue: Map<ModulePath, OwnerName>? by reportDataRepo.moduleToOwnerMap.collectAsState(null)

  val metadata by reportDataRepo.reportMetadata.collectAsState(null)

  val statKey = statsNavRoute.statKey
  val statsData: StatJsReportModel? by reportDataRepo.statForKey(statKey).collectAsState(null)

  if (moduleToOwnerMapFlowValue == null || metadata == null || statsData == null) {
    BootstrapLoadingSpinner()
    return
  }

  val statInfo = statsData!!.statInfo
  val statsByModule = statsData!!.statsByModule
  val allModules1: Set<ModulePath> = statsByModule.keys

  H1 {
    Text(buildString {
      append(statInfo.description)
    })
  }

  val query = statsNavRoute.moduleQuery

  val allModules = if (query != null && query != ":" && query.isNotEmpty()) {
    allModules1.filter { it.contains(query) }
  } else {
    allModules1
  }

  BootstrapSearchBox(
    query = query ?: "",
    placeholderText = "Module Filter...",
  ) {
    navRouteRepo.replaceNavRoute(statsNavRoute.copy(moduleQuery = it))
  }

  val SUPPORTED_TYPES = listOf(
    StatDataType.STRING,
    StatDataType.BOOLEAN,
    StatDataType.NUMERIC,
    StatDataType.CODE_REFERENCES
  )
  val resultsTab = BootstrapTabData("Results") {
    val statsColumns = mutableListOf<List<String>>().apply {
      if (SUPPORTED_TYPES.contains(statInfo.dataType)) {
        val value = allModules.map { gradlePath ->
          val statDataForModule = statsByModule[gradlePath]
          when (statDataForModule) {
            is Stat.BooleanStat -> {
              statDataForModule.value.toString()
            }

            is Stat.StringStat -> {
              statDataForModule.value
            }

            is Stat.NumericStat -> {
              statDataForModule.value.toString()
            }

            is Stat.CodeReferencesStat -> {
              if (metadata != null) {
                statDataForModule.value.toMarkdown(metadata!!)
              } else {
                statDataForModule.value.toString()
              }
            }

            else -> ""
          }
        }
        add(
          allModules.map { gradlePath ->
            moduleToOwnerMapFlowValue?.get(gradlePath) ?: ""
          }
        )
        add(value)
        add(
          allModules.map { gradlePath ->
            val stat: Stat? = statsByModule[gradlePath]
            statToDetailsString(stat)
          }
        )

      }
    }

    val headers = mutableListOf("Module")
      .apply {
        if (SUPPORTED_TYPES.contains(statInfo.dataType)) {
          if (!moduleToOwnerMapFlowValue.isNullOrEmpty()) {
            add("Owner")
          }
          val statDescription = statInfo.description
          add(statDescription)
          add("$statDescription Details")
        }
      }
    val values: List<List<String>> = allModules.mapIndexed { idx, modulePath ->
      mutableListOf(
        modulePath
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
          navRouteRepo.pushNavRoute(ModuleDetailNavRoute(cellValues[0]))
        }
      } else {
        H3 { Text("No Collected Stats of Type(s) ${statsNavRoute.statKey}") }
      }
    }
  }

  BootstrapTabPane(
    listOf(
      resultsTab,
    )
  )

}

fun statToDetailsString(stat: Stat?): String = when (stat) {
  is Stat.BooleanStat -> {
    stat.details
  }

  is Stat.StringStat -> {
    stat.details
  }

  is Stat.NumericStat -> {
    stat.details
  }

  is Stat.CodeReferencesStat -> {
    buildString {
      stat.value.forEach {
        appendLine("${it.code}")
      }
    }
  }

  else -> ""
} ?: ""


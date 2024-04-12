package ui

import PagingConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.ModuleDetailNavRoute
import com.squareup.invert.common.navigation.routes.StatDetailNavRoute
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatInfo
import com.squareup.invert.models.StatKey
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.Text


@Composable
fun StatDetailComposable(
  reportDataRepo: ReportDataRepo,
  navRouteRepo: NavRouteRepo,
  statsNavRoute: StatDetailNavRoute
) {
  val allPluginIds by reportDataRepo.allPluginIds.collectAsState(null)
  val statsData by reportDataRepo.statsData.collectAsState(null)
  val allModulesOrig by reportDataRepo.allModules.collectAsState(null)
  val moduleToOwnerMapFlowValue by reportDataRepo.moduleToOwnerMap.collectAsState(null)


  val statKeys = statsNavRoute.statKeys.ifEmpty {
    statsData?.statInfos?.map { it.key } ?: listOf()
  }
  H1 { Text("Stats") }

  if (moduleToOwnerMapFlowValue == null) {
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

  val resultsTab = BootstrapTabData("Results") {
    val statsColumns = mutableListOf<List<String>>().apply {
      statKeys.forEach { statKey ->
        val statInfo = statsData?.statInfos?.get(statKey)
        statInfo?.let { statInfo: StatInfo ->
          add(
            allModules.map { gradlePath ->
              val statsDataForModule: Map<StatKey, Stat>? = statsData?.statsByModule?.get(gradlePath)
              val stat = statsDataForModule?.get(statKey)
              when (stat) {
                is Stat.ClassDefinitionsStat -> {
                  val definitions = stat.definitions
                  if (definitions.isEmpty()) {
                    ""
                  } else {
                    definitions.size.toString() + " Type Definitions"
                  }
                }

                is Stat.HasImportStat -> {
                  stat.value.toString()
                }

                is Stat.StringToListStat -> {
                  buildString {
                    val scopeToBindings = stat.map
                    scopeToBindings.forEach { (key, values) ->
                      appendLine(key)
                      values.forEach { appendLine(it) }
                      appendLine()
                    }
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
                is Stat.ClassDefinitionsStat -> {
                  buildString {
                    stat.definitions
                      .map { it.fqName }
                      .forEach {
                        appendLine(it)
                      }
                  }
                }

                is Stat.HasImportStat -> {
                  stat.details ?: ""
                }

                else -> ""
              }

            }
          )
        }
      }
    }

    val headers = mutableListOf("Module")
      .apply {
        statKeys.forEach {
          add(it)
          add("$it Details")
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
          types = headers.map { String::class },
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
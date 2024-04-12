package ui

import PagingConstants.MAX_RESULTS
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.ReportDataRepo
import kotlinx.browser.window
import kotlin.reflect.KClass

@Composable
fun ConfigurationsComposable(reportDataRepo: ReportDataRepo) {
  val allAvailableConfigurationNamesOrig by reportDataRepo.allAvailableConfigurationNames.collectAsState(null)
  val allAvailableConfigurationNames = allAvailableConfigurationNamesOrig
  val listOrig by reportDataRepo.allAnalyzedConfigurationNames.collectAsState(null)
  val list = listOrig
  if (list == null || allAvailableConfigurationNames == null) {
    BootstrapLoadingMessageWithSpinner()
    return
  }

  val count = list.size
  TitleRow("Analyzed Gradle Configurations ($count of ${allAvailableConfigurationNames.size} Total)")


  BootstrapRow {
    BootstrapColumn(12) {
      BootstrapClickableList("Analyzed Configurations", list, MAX_RESULTS) { item ->
        window.alert("Clicked $item")
      }
    }
  }

  BootstrapTable(
    headers = listOf("Other (Not Analyzed) Configurations"),
    rows = mutableListOf<List<String>>().also { rows ->
      allAvailableConfigurationNames.forEach { availableConfigurationName ->
        val wasScanned = list.contains(availableConfigurationName)
        if (!wasScanned) {
          rows.add(
            mutableListOf(
              availableConfigurationName,
            )
          )
        }
      }
    },
    types = listOf<KClass<*>>(String::class),
    maxResultsLimitConstant = MAX_RESULTS,
    onItemClick = {
      window.alert("Clicked ${it[0]}")
    }
  )
}
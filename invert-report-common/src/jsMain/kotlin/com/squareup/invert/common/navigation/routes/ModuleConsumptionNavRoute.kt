package com.squareup.invert.common.navigation.routes

import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.models.ConfigurationName
import com.squareup.invert.models.GradlePluginId

data class ModuleConsumptionNavRoute(
  val pluginGroupByFilter: List<GradlePluginId> = listOf(),
  val configurations: List<ConfigurationName> = listOf(),
  val moduleQuery: String? = null,
) : BaseNavRoute(ModuleConsumption.pageId) {
  override fun toSearchParams(): Map<String, String> = toParamsWithOnlyPageId(this)
    .apply {
      if (pluginGroupByFilter.isNotEmpty()) {
        this[PLUGIN_GROUP_BY_FILTER_QUERY_PARAM_NAME] =
          pluginGroupByFilter.joinToString(separator = ",")
      }
      if (configurations.isNotEmpty()) {
        this[CONFIGURATIONS_QUERY_PARAM_NAME] = configurations.joinToString(separator = ",")
      }
      moduleQuery?.let {
        this[MODULE_QUERY_PARAM_NAME] = moduleQuery
      }
    }

  companion object {
    val ModuleConsumption = NavPage(
      pageId = "consumption",
      displayName = "Module Consumption",
      navIconSlug = "bar-chart",
      navRouteParser = { parser(it) }
    )

    private const val MODULE_QUERY_PARAM_NAME = "module"
    private const val PLUGIN_GROUP_BY_FILTER_QUERY_PARAM_NAME = "plugin_id"
    private const val CONFIGURATIONS_QUERY_PARAM_NAME = "configurations"
    fun parser(params: Map<String, String?>): ModuleConsumptionNavRoute = ModuleConsumptionNavRoute(
      pluginGroupByFilter = params[PLUGIN_GROUP_BY_FILTER_QUERY_PARAM_NAME]?.split(",")
        ?.filter { it.isNotBlank() } ?: listOf(),
      configurations = params[CONFIGURATIONS_QUERY_PARAM_NAME]?.split(",")
        ?.filter { it.isNotBlank() }
        ?: listOf(),
      moduleQuery = params[MODULE_QUERY_PARAM_NAME]
    )
  }
}

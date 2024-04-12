package com.squareup.invert.common.navigation.routes

import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.models.GradlePluginId

data class StatDetailNavRoute(
  val pluginIds: List<GradlePluginId>,
  val statKeys: List<String>,
  val moduleQuery: String? = null
) : BaseNavRoute(StatDetail.pageId) {

  override fun toSearchParams(): Map<String, String> = toParamsWithOnlyPageId(this)
    .also { params ->
      params[PLUGIN_IDS_PARAM] = pluginIds.joinToString(separator = ",")
      params[STATKEYS_PARAM] = statKeys.joinToString(separator = ",")
      moduleQuery?.let {
        params[MODULE_QUERY_PARAM] = it
      }
    }

  companion object {
    val StatDetail = NavPage(
      pageId = "stat_detail",
      navRouteParser = { parser(it) }
    )

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

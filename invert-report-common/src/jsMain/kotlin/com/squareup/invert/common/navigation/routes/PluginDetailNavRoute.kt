package com.squareup.invert.common.navigation.routes

import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.models.GradlePluginId

data class PluginDetailNavRoute(
  val pluginId: GradlePluginId,
) : BaseNavRoute(PluginDetail.pageId) {
  override fun toSearchParams() = toParamsWithOnlyPageId(this)
    .apply {
      this[PLUGIN_ID_PARAM] = pluginId
    }

  companion object {
    val PluginDetail = NavPage(
      pageId = "plugin_detail",
      navRouteParser = { parser(it) }
    )

    const val PLUGIN_ID_PARAM = "id"
    fun parser(params: Map<String, String?>): NavRoute {
      params[PLUGIN_ID_PARAM]?.let {
        return PluginDetailNavRoute(it)
      }
      return PluginsNavRoute()
    }
  }
}

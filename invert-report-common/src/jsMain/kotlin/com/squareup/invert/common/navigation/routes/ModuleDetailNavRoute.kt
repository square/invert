package com.squareup.invert.common.navigation.routes

import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.models.ConfigurationName
import com.squareup.invert.models.GradlePath

data class ModuleDetailNavRoute(
  val path: GradlePath,
  val configurationName: ConfigurationName? = null,
) : BaseNavRoute(NavPage(
  pageId = "module_detail",
  navRouteParser = { parser(it) }
)) {
  override fun toSearchParams() = toParamsWithOnlyPageId(this)
    .also { map ->
      map[PATH_PARAM] = path
      configurationName?.let {
        map[CONFIGURATION_PARAM] = it
      }
    }

  companion object {

    private const val PATH_PARAM = "path"
    private const val CONFIGURATION_PARAM = "configuration"
    fun parser(params: Map<String, String?>): NavRoute {
      val path = params[PATH_PARAM]
      val configurationName = params[CONFIGURATION_PARAM]
      return if (!path.isNullOrEmpty()) {
        ModuleDetailNavRoute(
          path = path,
          configurationName = configurationName
        )
      } else {
        AllModulesNavRoute()
      }
    }
  }
}

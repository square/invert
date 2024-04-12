package com.squareup.invert.common.navigation.routes

import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRoute


class UnusedModulesNavRoute : BaseNavRoute(UnusedModules.pageId) {
  override fun toSearchParams() = toParamsWithOnlyPageId(this)

  companion object {
    val UnusedModules = NavPage.UnusedModules
    fun parser(params: Map<String, String?>): NavRoute {
      return UnusedModulesNavRoute()
    }
  }
}

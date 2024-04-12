package com.squareup.invert.common.navigation.routes

import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRoute


class LeafModulesNavRoute : BaseNavRoute(LeafModules.pageId) {
  override fun toSearchParams() = toParamsWithOnlyPageId(this)

  companion object {
    val LeafModules = NavPage.LeafModules
    fun parser(params: Map<String, String?>): NavRoute {
      return LeafModulesNavRoute()
    }
  }
}

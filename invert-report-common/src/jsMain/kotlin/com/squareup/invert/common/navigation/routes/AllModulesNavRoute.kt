package com.squareup.invert.common.navigation.routes

import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavPage.Companion.AllModules
import com.squareup.invert.common.navigation.NavRoute


data class AllModulesNavRoute(
  val query: String? = null,
) : BaseNavRoute(AllModules.pageId) {
  override fun toSearchParams() = toParamsWithOnlyPageId(this)
    .also { map ->
      query?.let {
        map[QUERY_PARAM] = it
      }
    }

  companion object {
    val AllModules = NavPage.AllModules

    private const val QUERY_PARAM = "query"
    fun parser(params: Map<String, String?>): NavRoute {
      val queryParam = params[QUERY_PARAM]
      return AllModulesNavRoute(queryParam)
    }
  }
}

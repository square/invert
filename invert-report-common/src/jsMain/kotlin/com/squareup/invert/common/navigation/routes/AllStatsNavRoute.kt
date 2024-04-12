package com.squareup.invert.common.navigation.routes

import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.models.CollectedStatType

data class AllStatsNavRoute(
  val statType: CollectedStatType? = null
) : BaseNavRoute(
  AllStats.pageId
) {
  override fun toSearchParams(): Map<String, String> = toParamsWithOnlyPageId(this)
    .also { params ->
      statType?.name?.let {
        params[STAT_TYPE_PARAM] =
          it
      }
    }

  companion object {

    val AllStats = NavPage(
      pageId = "stats",
      displayName = "Stats",
      navIconSlug = "pie-chart",
      navRouteParser = { parser(it) }
    )

    private const val STAT_TYPE_PARAM = "types"

    fun parser(params: Map<String, String?>): AllStatsNavRoute {
      val statTypeParam = params[STAT_TYPE_PARAM]
      val statType = CollectedStatType.entries.firstOrNull { it.name == statTypeParam }
      return AllStatsNavRoute(
        statType = statType,
      )
    }
  }
}

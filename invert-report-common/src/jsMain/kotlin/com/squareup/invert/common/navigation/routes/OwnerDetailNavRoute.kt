package com.squareup.invert.common.navigation.routes

import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.models.OwnerName

data class OwnerDetailNavRoute(
  val owner: OwnerName,
) : BaseNavRoute(OwnerDetail.pageId) {

  override fun toSearchParams(): Map<String, String> = toParamsWithOnlyPageId(this)
    .also { map ->
      map[OWNER_PARAM] = owner
    }

  companion object {


    val OwnerDetail = NavPage(
      pageId = "owner_detail",
      navRouteParser = { parser(it) }
    )

    private const val OWNER_PARAM = "owner"
    fun parser(params: Map<String, String?>): NavRoute {
      val owner = params[OWNER_PARAM]
      return if (!owner.isNullOrEmpty()) {
        OwnerDetailNavRoute(owner)
      } else {
        OwnersNavRoute()
      }
    }
  }
}

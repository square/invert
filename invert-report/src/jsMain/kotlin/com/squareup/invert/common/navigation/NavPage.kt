package com.squareup.invert.common.navigation

typealias NavPageId = String

class NavPage(
  val pageId: NavPageId,
  val navRouteParser: (params: Map<String, String?>) -> NavRoute,
  val displayName: String = pageId,
  val navIconSlug: String = "arrow-right-circle",
) {

  data class NavItem(
    val itemTitle: String,
    val navPage: NavPage? = null,
    val destinationNavRoute: NavRoute = navPage?.navRouteParser?.invoke(mapOf())
      ?: throw RuntimeException("Requires destinationNavRoute"),
    val matchesCurrentNavRoute: (NavRoute) -> Boolean = { currentNavRoute ->
      currentNavRoute::class == destinationNavRoute::class
    },
    val navIconSlug: String = navPage?.navIconSlug ?: "record"
  )
}
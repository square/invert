package com.squareup.invert.common.navigation

import history.PushOrReplaceState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Source of truth for the current [NavRoute]
 */
class NavRouteRepo(initialRoute: NavRoute) {

  private val _navRoute = MutableStateFlow(NavChangeEvent(initialRoute))

  val navRoute: Flow<NavChangeEvent> = _navRoute

  fun updateNavRoute(navRoute: NavRoute) {
    this._navRoute.tryEmit(NavChangeEvent(navRoute))
  }

  fun replaceNavRoute(navRoute: NavRoute) {
    this._navRoute.tryEmit(NavChangeEvent(navRoute, PushOrReplaceState.REPLACE))
  }

  class NavChangeEvent(
    val navRoute: NavRoute,
    val pushOrReplaceState: PushOrReplaceState = PushOrReplaceState.PUSH
  )
}

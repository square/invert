package com.squareup.invert.common.navigation

import history.PushOrReplaceState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Source of truth for the current [NavRoute]
 */
class NavRouteRepo(initialRoute: NavRoute) {

  private val _navRoute = MutableStateFlow(
    NavChangeEvent(
      initialRoute,
      PushOrReplaceState.PUSH
    )
  )

  val navRoute: Flow<NavChangeEvent> = _navRoute

  /**
   * Adds new item to browser history stack.
   */
  fun pushNavRoute(navRoute: NavRoute) {
    this._navRoute.tryEmit(NavChangeEvent(navRoute, PushOrReplaceState.PUSH))
  }

  /**
   * Updates/Replaces the URL in the browser history, but does NOT add a new item to browser history stack.
   */
  fun replaceNavRoute(navRoute: NavRoute) {
    this._navRoute.tryEmit(NavChangeEvent(navRoute, PushOrReplaceState.REPLACE))
  }

  class NavChangeEvent(
    val navRoute: NavRoute,
    val pushOrReplaceState: PushOrReplaceState,
  )
}

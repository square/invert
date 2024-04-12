package com.squareup.invert.common.navigation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Source of truth for the current [NavRoute]
 */
class NavRouteRepo(initialRoute: NavRoute) {

  private val _navRoute = MutableStateFlow(initialRoute)

  val navRoute: Flow<NavRoute> = _navRoute

  fun updateNavRoute(navRoute: NavRoute) {
    this._navRoute.tryEmit(navRoute)
  }
}

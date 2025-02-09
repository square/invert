package com.squareup.invert.common.navigation.routes

import com.squareup.invert.common.navigation.NavRoute

fun NavRoute.toQueryString(): String {
  return "?" + toSearchParams().map { (key, value) -> "$key=$value" }.joinToString("&")
}

package com.squareup.invert.common.navigation

/**
 * Represents a page that can be navigated to.
 */
interface NavRoute {

  /**
   * Unique Identifier (lower_snake_case) for this [NavRoute].
   * It is assumed that [NavPageId] is unique for each [NavRoute]
   */
  val page: NavPageId

  /**
   * Takes this [NavRoute] and converts it in to a key/value map that can be serialized into
   * a JSON object for browser history, as well as a url query string for deep-linking.
   */
  fun toSearchParams(): Map<String, String>
}

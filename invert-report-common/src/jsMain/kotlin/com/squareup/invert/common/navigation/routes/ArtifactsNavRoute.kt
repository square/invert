package com.squareup.invert.common.navigation.routes

import com.squareup.invert.common.navigation.NavPage

data class ArtifactsNavRoute(
  val query: String? = null,
) : BaseNavRoute(Artifacts.pageId) {

  override fun toSearchParams(): Map<String, String> = toParamsWithOnlyPageId(this)
    .also { params ->
      query?.let {
        params[QUERY_PARAM] = query
      }
    }

  companion object {

    val Artifacts = NavPage(
      pageId = "artifacts",
      displayName = "Artifacts",
      navIconSlug = "newspaper",
      navRouteParser = { parser(it) }
    )

    private const val QUERY_PARAM = "query"

    fun parser(params: Map<String, String?>): ArtifactsNavRoute {
      return ArtifactsNavRoute(
        query = params[QUERY_PARAM]
      )
    }
  }
}

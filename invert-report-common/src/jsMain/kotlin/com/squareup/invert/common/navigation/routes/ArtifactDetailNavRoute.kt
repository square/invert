package com.squareup.invert.common.navigation.routes

import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRoute

data class ArtifactDetailNavRoute(
  val group: String,
  val artifact: String,
  val version: String? = null,
) : BaseNavRoute(ArtifactDetail.pageId) {
  override fun toSearchParams(): Map<String, String> = toParamsWithOnlyPageId(this)
    .also { map ->
      map[GROUP_PARAM] = group
      map[ARTIFACT_PARAM] = artifact
      version?.let {
        map[VERSION_PARAM] = it
      }
    }

  companion object {
    val ArtifactDetail = NavPage(
      pageId = "artifact_detail",
      navRouteParser = { parser(it) }
    )

    private const val GROUP_PARAM = "g"
    private const val ARTIFACT_PARAM = "a"
    private const val VERSION_PARAM = "v"
    fun parser(params: Map<String, String?>): NavRoute {
      val groupParam = params[GROUP_PARAM]
      val artifactParam = params[ARTIFACT_PARAM]
      return if (groupParam != null && artifactParam != null) {
        return ArtifactDetailNavRoute(
          group = groupParam,
          artifact = artifactParam,
          version = params[VERSION_PARAM],
        )
      } else {
        ArtifactsNavRoute()
      }
    }
  }
}

package com.squareup.invert.common.navigation.routes

import com.squareup.invert.common.navigation.NavPage

class AnnotationProcessorsNavRoute : BaseNavRoute(AnnotationProcessors.pageId) {
  companion object {
    val AnnotationProcessors = NavPage(
      pageId = "annotation_processors",
      displayName = "Annotation Processors",
      navIconSlug = "cpu",
      navRouteParser = { AnnotationProcessorsNavRoute() }
    )
  }
}

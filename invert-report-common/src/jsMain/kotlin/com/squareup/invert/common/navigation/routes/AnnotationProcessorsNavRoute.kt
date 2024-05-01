package com.squareup.invert.common.navigation.routes

import com.squareup.invert.common.navigation.NavPage

object AnnotationProcessorsNavRoute : BaseNavRoute(
    NavPage(
        pageId = "annotation_processors",
        displayName = "Annotation Processors",
        navIconSlug = "cpu",
        navRouteParser = { AnnotationProcessorsNavRoute }
    )
)
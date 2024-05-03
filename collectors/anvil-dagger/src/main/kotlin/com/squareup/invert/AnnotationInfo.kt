package com.squareup.invert

/**
 * Metadata for a parsed Kotlin Annotation
 *
 * @param type is the Fully Qualified Classname of the Annotation
 * @param args is the list of arguments provided on the Annotation
 */
data class AnnotationInfo(
    val type: String,
    val args: List<AnnotationArg>?
)
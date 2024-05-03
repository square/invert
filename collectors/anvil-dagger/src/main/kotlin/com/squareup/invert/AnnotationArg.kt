package com.squareup.invert

/**
 * Represents a parsed Annotation argument on a Kotlin Class.
 */
sealed interface AnnotationArg {
    val name: String?

    /**
     * An argument with a single value
     */
    data class AnnotationArgSingle(
        override val name: String?,
        val value: String
    ) : AnnotationArg

    /**
     * An annotation argument with an array of values
     */
    data class AnnotationArgArray(
        override val name: String?,
        val values: List<String>
    ) : AnnotationArg
}
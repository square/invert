package com.squareup.invert.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * All types of [Stat]s supported by Invert.
 *
 * The [SerialName] attribute is used for polymorphism with KotlinX Serialization
 */
sealed interface Stat {


    @Serializable
    @SerialName("numeric_stat")
    data class NumericStat(
        val value: Int,
        val details: String? = null,
    ) : Stat

    @Serializable
    @SerialName("string_stat")
    data class StringStat(
        val value: String,
        val details: String? = null,
    ) : Stat

    @Serializable
    @SerialName("boolean_stat")
    data class BooleanStat(
        val value: Boolean,
        val details: String? = null,
    ) : Stat


    @Serializable
    @SerialName("di_provides_and_injects")
    data class DiProvidesAndInjectsStat(
        val value: List<ProvidesAndInjects>,
        val details: String? = null,
    ) : Stat {
        /**
         * Represents the data in an Anvil ContributesBinding Annotation Usage
         */
        @Serializable
        data class DiContribution(
            val annotation: String,
            val scope: String,
            val boundImplementation: String,
            val boundType: String,
            val replaces: List<String>,
        )

        /**
         * Represents the data in an Anvil ContributesBinding Annotation Usage
         */
        @Serializable
        data class DiInjection(
            val type: String,
            val qualifierAnnotations: List<String>,
            val startLine: Int,
            val endLine: Int,
        )

        @Serializable
        data class ProvidesAndInjects(
            val classFqName: String,
            val contributions: List<DiContribution>,
            val consumptions: List<DiInjection>,
            val filePath: String,
            val startLine: Int,
            val endLine: Int,
        )
    }
}

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
    @SerialName("class_definitions_stat")
    data class ClassDefinitionsStat(
        val definitions: List<ClassDefinition>
    ) : Stat {
        @Serializable
        data class ClassDefinition(
            val name: String,
            val fqName: String,
            val file: String,
            val supertypes: List<String>,
        )
    }

    @Serializable
    @SerialName("string_stat")
    data class StringStat(
        val stat: String,
    ) : Stat

    @Serializable
    @SerialName("has_import_stat")
    data class HasImportStat(
        val value: Boolean,
        val details: String? = null,
    ) : Stat


    @Serializable
    @SerialName("provides_and_injects")
    data class ProvidesAndInjectsStat(
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

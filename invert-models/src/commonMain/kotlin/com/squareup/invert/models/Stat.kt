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
}

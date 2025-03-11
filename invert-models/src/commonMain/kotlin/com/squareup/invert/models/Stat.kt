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
  @SerialName("code_reference")
  data class CodeReferencesStat(
    val value: List<CodeReference>,
  ) : Stat {

    @Serializable
    data class CodeReference(
      val filePath: String,
      val startLine: Int,
      val endLine: Int,
      val extras: Map<ExtraKey, String> = emptyMap(),
      val code: Markdown? = null,
      val owner: OwnerName? = null,
    )
  }
}
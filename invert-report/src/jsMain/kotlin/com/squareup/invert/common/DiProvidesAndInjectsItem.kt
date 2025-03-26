package com.squareup.invert.common

import com.squareup.invert.models.ModulePath

data class DiKey(
  val type: String
) {
  override fun toString(): String {
    return type
  }
}

sealed interface DiProvidesAndInjectsItem {
  data class Provides(
    val module: ModulePath,
    val filePath: String,
    val startLine: Int,
    val endLine: Int,
    val type: String,
    val implementationType: String,
    val scope: String? = null,
    val qualifiers: List<String> = emptyList(),
  ) : DiProvidesAndInjectsItem {
    val key = DiKey(
      type = type
    )
  }

  data class Injects(
    val module: ModulePath,
    val filePath: String,
    val startLine: Int,
    val endLine: Int,
    val type: String,
    val qualifiers: List<String>,
  ) : DiProvidesAndInjectsItem {
    val key = DiKey(
      type = type
    )
  }
}
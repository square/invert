package com.squareup.af.analysis.stats.classdefinition.model

data class ClassDefinitionInfo(
  val name: String,
  val fqName: String,
  val file: String,
  val supertypes: List<String>,
)

package com.squareup.invert.internal

sealed interface Dependency {
  val name: String

  data class ModuleDependency(
    val path: String,
  ) : Dependency {
    override val name: String get() = path
  }

  data class ArtifactDependency(
    val group: String,
    val artifact: String,
    val version: String,
  ) : Dependency {
    override val name: String get() = "$group:$artifact:$version"
  }
}

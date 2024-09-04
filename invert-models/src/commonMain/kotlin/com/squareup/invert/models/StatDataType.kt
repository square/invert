package com.squareup.invert.models

import kotlinx.serialization.Transient
import kotlin.reflect.KClass

enum class StatDataType(
  @Transient val backingType: KClass<*>
) {
  BOOLEAN(Boolean::class),
  NUMERIC(Long::class),
  STRING(String::class),
  CODE_REFERENCES(Stat.CodeReferencesStat.CodeReference::class), // EXPLORE (CUSTOM)
  DI_PROVIDES_AND_INJECTS(Stat.DiProvidesAndInjectsStat::class); // EXPLORE (CUSTOM)

  companion object {
    fun fromString(type: String?): StatDataType? {
      return StatDataType.entries.firstOrNull { it.name == type }
    }
  }

}

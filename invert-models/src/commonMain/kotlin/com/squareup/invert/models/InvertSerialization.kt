package com.squareup.invert.models

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

object InvertSerialization {
  /**
   * This instance should be used when serializing to JSON with Invert.
   *
   * This binds serializers of interfaces to concrete classes.
   */
  val InvertJson = Json {
    ignoreUnknownKeys = true
    serializersModule = SerializersModule {
      polymorphic(
        baseClass = Stat::class,
        actualClass = Stat.HasImportStat::class,
        actualSerializer = Stat.HasImportStat.serializer()
      )
      polymorphic(
        baseClass = Stat::class,
        actualClass = Stat.ClassDefinitionsStat::class,
        actualSerializer = Stat.ClassDefinitionsStat.serializer()
      )
      polymorphic(
        baseClass = Stat::class,
        actualClass = Stat.GenericStat::class,
        actualSerializer = Stat.GenericStat.serializer()
      )
    }
  }
}

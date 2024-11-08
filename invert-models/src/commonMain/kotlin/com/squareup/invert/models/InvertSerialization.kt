package com.squareup.invert.models

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.modules.SerializersModule

object InvertSerialization {

  fun JsonBuilder.applyCommonConfig() {
    allowStructuredMapKeys = true
    ignoreUnknownKeys = true
    serializersModule = SerializersModule {
      // Stat
      polymorphic(
        baseClass = Stat::class,
        actualClass = Stat.BooleanStat::class,
        actualSerializer = Stat.BooleanStat.serializer()
      )
      polymorphic(
        baseClass = Stat::class,
        actualClass = Stat.StringStat::class,
        actualSerializer = Stat.StringStat.serializer()
      )
      polymorphic(
        baseClass = Stat::class,
        actualClass = Stat.NumericStat::class,
        actualSerializer = Stat.NumericStat.serializer()
      )
      polymorphic(
        baseClass = Stat::class,
        actualClass = Stat.DiProvidesAndInjectsStat::class,
        actualSerializer = Stat.DiProvidesAndInjectsStat.serializer()
      )
      polymorphic(
        baseClass = Stat::class,
        actualClass = Stat.CodeReferencesStat::class,
        actualSerializer = Stat.CodeReferencesStat.serializer()
      )
    }
  }

  /**
   * This instance should be used when serializing to JSON with Invert.
   *
   * This binds serializers of interfaces to concrete classes.
   */
  val InvertJson = Json {
    applyCommonConfig()
  }

  /** Use this only for DEBUGGING */
  val InvertJsonPrettyPrint = Json {
    applyCommonConfig()
    prettyPrint = true
  }
}

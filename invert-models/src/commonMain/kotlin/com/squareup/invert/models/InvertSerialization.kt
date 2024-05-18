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
    }
  }
}

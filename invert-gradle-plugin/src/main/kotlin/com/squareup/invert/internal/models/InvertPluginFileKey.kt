package com.squareup.invert.internal.models

/**
 * Represents a JSON output file from Invert.  This is used for saving and loading of these files
 * in a type-safe way.
 */
enum class InvertPluginFileKey(
  val filename: String,
  val description: String
) {
  DEPENDENCIES("dependencies.json", "Dependencies"),
  CONFIGURATIONS("configurations.json", "Configurations"),
  PLUGINS("plugins.json", "Plugins"),
  OWNERS("owners.json", "Owners"),
  METADATA("metadata.json", "Metadata"),
  STATS("stats.json", "Stats"),
  STAT_TOTALS("stat_totals.json", "Stat Totals"),
}

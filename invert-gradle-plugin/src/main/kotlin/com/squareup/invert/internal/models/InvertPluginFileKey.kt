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
  HISTORICAL_DATA("historical_data.json", "Historical Data"),
  PLUGINS("plugins.json", "Plugins"),
  OWNERS("owners.json", "Owners"),
  METADATA("metadata.json", "Metadata"),
  STATS("stats.json", "Stats"),
  STATS_SARIF("stats.sarif", "Stats"),
  STAT_TOTALS("stat_totals.json", "Stat Totals"),
}

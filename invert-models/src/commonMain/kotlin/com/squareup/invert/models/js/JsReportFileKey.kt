package com.squareup.invert.models.js

/**
 * Represents a JavaScript output file from Invert.  This is used for saving and loading of these files
 * in a type-safe way.
 */
enum class JsReportFileKey(
  val key: String,
  val description: String
) {
  DIRECT_DEPENDENCIES("direct_dependencies", "Direct Dependencies"),
  INVERTED_DEPENDENCIES("inverted_dependencies", "Inverted Dependencies"),
  HISTORICAL_DATA("historical_stats", "Historical Stats"),
  HOME("home", "Home"),
  PLUGINS("plugins", "Plugins"),
  OWNERS("owners", "Owners"),
  METADATA("metadata", "Metadata"),
  CONFIGURATIONS("configurations", "Configurations"),
  STAT("stat", "Stat"),
  STATS("stats", "Stats"),
  STAT_TOTALS("stat_totals", "Stat Totals");

  val jsFilename = "$key.js"
}

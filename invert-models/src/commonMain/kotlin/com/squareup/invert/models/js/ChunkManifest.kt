package com.squareup.invert.models.js

import kotlinx.serialization.Serializable

/**
 * Manifest describing a chunked stat payload.
 *
 * When a stat file exceeds a size threshold, the writer emits a manifest file alongside
 * multiple chunk files. The runtime can fetch this manifest, load chunks in parallel,
 * and merge them back into a single [StatJsReportModel].
 */
@Serializable
data class ChunkManifest(
  /** Schema version for forward compatibility. */
  val version: Int = 1,
  /** The report file key this manifest describes (e.g. "stat_my_stat_key"). */
  val key: String,
  /** Total number of chunk files. */
  val totalChunks: Int,
  /** Ordered list of chunk filenames relative to the js/ directory. */
  val chunkFiles: List<String>,
)

package com.squareup.invert

import com.squareup.invert.models.ModulePath
import java.io.File

/**
 * Provided to [StatCollector]s during collection.
 */
class InvertCollectContext(
  val gitCloneDir: File,
  val modulePath: ModulePath,
  val moduleDir: File,
)

val InvertCollectContext.projectSrcDir get() = File(moduleDir, "src")

val InvertCollectContext.sourceFiles
  get() = projectSrcDir.walkTopDown().filter { file -> file.isFile }

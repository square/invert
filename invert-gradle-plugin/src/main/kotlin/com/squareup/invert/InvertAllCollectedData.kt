package com.squareup.invert

import com.squareup.invert.models.GradlePath
import java.io.File

/**
 * Information sent to a [StatCollector] for a specific project/module.
 */
class InvertAllCollectedData(
  val rootProjectDir: File,
  val projectPath: GradlePath,
  val projectDir: File,
)

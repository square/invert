package com.squareup.invert

import com.squareup.invert.models.GradlePath
import java.io.File


class InvertProjectData(
  val rootProjectDir: File,
  val projectPath: GradlePath,
  val projectDir: File,
)

val InvertProjectData.projectSrcDir get() = File(projectDir, "src")

val InvertProjectData.sourceFiles
  get() = projectSrcDir.walkTopDown().filter { file -> file.isFile }

fun File.relativePath(rootProjectDir: File): String = this.absolutePath.replace(rootProjectDir.absolutePath, "").drop(1)
package com.squareup.invert

import com.squareup.invert.models.GradlePath
import com.squareup.invert.models.GradlePluginId
import java.io.File


class InvertProjectData(
  val rootProjectDir: File,
  val projectPath: GradlePath,
  val projectDir: File,
  val pluginIds: List<GradlePluginId> = emptyList(),
)


val InvertProjectData.projectSrcDir get() = File(projectDir, "src")
val InvertProjectData.sourceFiles
  get() = projectSrcDir.walkTopDown().filter { file -> file.isFile.also { println(file.absolutePath) } }

fun File.relativePath(rootProjectDir: File): String = this.absolutePath.replace(rootProjectDir.absolutePath, "").drop(1)
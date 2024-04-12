package stacktracer

import kotlinx.serialization.Serializable

/**
 * Data class holding A file, it's owner, path, gradle module and all symbols in that file.
 */
@Serializable
data class DefinitionToFile(
  val fileName: String,
  val owner: String,
  val relativePath: String,
  val packageName: String,
  val symbols: Set<String>,
  val gradlePath: String,
)

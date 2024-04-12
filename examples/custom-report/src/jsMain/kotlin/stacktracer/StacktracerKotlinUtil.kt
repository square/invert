package stacktracer

object StacktracerKotlinUtil {

  fun locateKotlinFile(
    stacktraceLine: String,
    allCollected: Set<DefinitionToFile>
  ): DefinitionToFile? {
    val cleanedLine = cleanKotlinStacktraceLine(stacktraceLine)
    println("Looking For Stack Trace Element Original $stacktraceLine")
    println("Looking For Stack Trace Element Cleaned  $cleanedLine")
    allCollected.forEach {
      val found = it.symbols.any { symbol ->
        symbol == cleanedLine
      }
      if (found) {
        println("Found in ${it.relativePath}")
        return it
      }
    }
    return null
  }

  fun cleanKotlinStacktraceLine(stacktraceLine: String): String {
    var cleanedLine = stacktraceLine
      .replaceAfter("Kt$", "")
      .replace("Kt$", "")
      .replaceAfter("$", "")
      .replace(
        ".access$",
        ""
      ) // Synthetic Accessors https://medium.com/@iateyourmic/synthetic-accessors-in-kotlin-a60184afd94e
      .replace("$", "")
      .replace("Kt.", ".")
      .replace("_Factory.newInstance", "") // Dagger Generated Factory

    if (cleanedLine.endsWith(".")) {
      cleanedLine = cleanedLine.dropLast(".".length)
    }
    return cleanedLine
  }
}

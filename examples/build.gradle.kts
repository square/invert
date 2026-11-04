buildscript {
  repositories {
    mavenLocal()
    mavenCentral()
    google()
    gradlePluginPortal()
    maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") }
  }
  dependencies {
    val invertVersion = "0.0.5-dev-SNAPSHOT"
    classpath("com.squareup.invert:invert-gradle-plugin:$invertVersion")
    classpath("com.squareup.invert:invert-collectors:$invertVersion")
    classpath("com.squareup.invert:invert-owners-github:$invertVersion")
  }
}

allprojects {
  repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
    maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") }
  }
}

plugins {
  id("com.squareup.invert")
}

/**
 * Collects every Kotlin import as a code reference, with two extras that Tech Debt Initiatives
 * can filter on:
 * - `package`: the imported package, e.g. `io.ktor.client` for `io.ktor.client.*` or
 *   `io.ktor.client.HttpClient`.
 * - `wildcard`: `true` for `*` imports, otherwise `false`.
 */
class KotlinImportsStatCollector : com.squareup.invert.StatCollector {
  private val importRegex = Regex("""^import\s+([\w.]+?)(\.\*|\.\w+)?(\s+as\s+\w+)?\s*$""")

  override fun collect(
    invertCollectContext: com.squareup.invert.InvertCollectContext,
  ): List<com.squareup.invert.CollectedStat>? {
    val codeReferences = invertCollectContext.moduleDir.resolve("src")
      .walkTopDown()
      .filter { it.isFile && it.extension == "kt" }
      .flatMap { sourceFile ->
        val filePath = sourceFile.relativeTo(invertCollectContext.gitCloneDir).path
        sourceFile.readLines().asSequence().mapIndexedNotNull { index, line ->
          val match = importRegex.matchEntire(line.trim()) ?: return@mapIndexedNotNull null
          val (path, lastSegment) = match.destructured
          val isWildcard = lastSegment == ".*"
          com.squareup.invert.models.Stat.CodeReferencesStat.CodeReference(
            filePath = filePath,
            startLine = index + 1,
            endLine = index + 1,
            code = line.trim(),
            extras = mapOf(
              "package" to path,
              "wildcard" to isWildcard.toString(),
            ),
          )
        }
      }
      .toList()
    if (codeReferences.isEmpty()) return null
    return listOf(
      com.squareup.invert.CollectedStat(
        metadata = com.squareup.invert.models.StatMetadata(
          key = "kotlin_imports",
          title = "Kotlin Imports",
          description = "Every Kotlin import, with its package and whether it is a wildcard import.",
          dataType = com.squareup.invert.models.StatDataType.CODE_REFERENCES,
          category = "Imports",
          extras = listOf(
            com.squareup.invert.models.ExtraMetadata(
              key = "package",
              type = com.squareup.invert.models.ExtraDataType.STRING,
              description = "Imported package",
            ),
            com.squareup.invert.models.ExtraMetadata(
              key = "wildcard",
              type = com.squareup.invert.models.ExtraDataType.BOOLEAN,
              description = "Wildcard import",
            ),
          ),
        ),
        stat = com.squareup.invert.models.Stat.CodeReferencesStat(codeReferences),
      )
    )
  }

  override fun getName(): String = "kotlin-imports"
}

invert {
  ownershipCollector(com.squareup.invert.owners.GitHubCodeOwnersInvertOwnershipCollector)
  addStatCollector(
    com.squareup.invert.collectors.linesofcode.LinesOfCodeStatCollector(
      name = "Kotlin",
      fileExtensions = listOf("kt", "kts"),
    )
  )
  addStatCollector(
    com.squareup.invert.collectors.contains.InvertContainsStatCollector(
      statKey = "wildcard-imports",
      statTitle = "Wildcard Imports",
      linePredicate = { it.contains("import") && it.contains("*") },
      filePredicate = { it.extension == "kt" || it.extension == "kts" },
    )
  )
  addStatCollector(KotlinImportsStatCollector())

  // Tech Debt Initiatives. After `./gradlew :invert`, open build/reports/invert/index.html and
  // view one with `?page=stat_breakdown&remaining=<effectiveRemainingStatKey>&completed=<effectiveCompletedStatKey>`.
  // json/tdi_config.json lists each initiative's effective keys.

  // A burndown of an existing stat, without filters.
  addTechDebtInitiative(
    com.squareup.invert.models.js.TechDebtInitiative(
      id = "remove_wildcard_imports",
      title = "Remove Wildcard Imports",
      descriptionMarkdown = "Replace every wildcard import with explicit imports.",
      unit = "Imports",
      completedStatKey = null,
      remainingStatKey = "wildcard-imports",
    )
  )

  // Both stats filtered from one stat, by extras. These are published as the derived stats
  // `tdi__explicit_ktor_imports__remaining` and `tdi__explicit_ktor_imports__completed`.
  addTechDebtInitiative(
    com.squareup.invert.models.js.TechDebtInitiative(
      id = "explicit_ktor_imports",
      title = "Explicit Ktor Imports",
      descriptionMarkdown = "Import Ktor declarations explicitly instead of with `io.ktor.*` wildcards.",
      unit = "Imports",
      completedStatKey = "kotlin_imports",
      remainingStatKey = "kotlin_imports",
      remainingFilter = com.squareup.invert.models.StatFilter(
        extras = mapOf(
          "package" to com.squareup.invert.models.ValueMatcher.regex("""^io\.ktor(\.|$)"""),
          "wildcard" to com.squareup.invert.models.ValueMatcher.equalTo("true"),
        ),
      ),
      completedFilter = com.squareup.invert.models.StatFilter(
        extras = mapOf(
          "package" to com.squareup.invert.models.ValueMatcher.regex("""^io\.ktor(\.|$)"""),
          "wildcard" to com.squareup.invert.models.ValueMatcher.equalTo("true").negated(),
        ),
      ),
    )
  )
}

/**
 * Will copy the report js files into invert so we can use them in dev cycles
 */
tasks.register("copyJsFiles") {
  doLast {
    copy {
      from(fileTree("build/reports/invert/js") { include("*.js") }) // Replace "src/js" with your source directory
      into("../invert-report/src/jsMain/resources/js") // Replace "build/js" with your target directory
    }
  }
}

afterEvaluate {
  rootProject.tasks.findByName("invert")!!.finalizedBy("copyJsFiles")
}

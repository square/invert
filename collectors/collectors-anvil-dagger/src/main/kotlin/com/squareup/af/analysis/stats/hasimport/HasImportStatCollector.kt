package com.squareup.af.analysis.stats.hasimport

import com.squareup.invert.CollectedStat
import com.squareup.invert.StatCollector
import com.squareup.invert.models.Stat.BooleanStat
import com.squareup.invert.models.StatMetadata
import com.squareup.psi.toKtFile
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

/**
 * Layer on top of [HasImportStatCollector] that helps collect data for Square's android-register.
 */
abstract class HasImportStatCollector(
  private val statInfo: StatMetadata,
  /** Will succeed if ANY of these imports match */
  private val desiredImports: List<String>
) : StatCollector {
  override fun collect(
    rootProjectFolder: File,
    projectPath: String,
    kotlinSourceFiles: List<File>,
  ): List<CollectedStat>? {
    return collectFromFileContents(kotlinSourceFiles.map { it.toKtFile() })
  }

  private fun collectFromFileContents(
    kotlinSourceFileContents: List<KtFile>
  ): List<CollectedStat>? {
    val referencedInFiles = kotlinSourceFileContents
      .filter { ktFile ->
        ktFile.importDirectives.any { importDirective ->
          val importPath = importDirective.importPath?.pathStr.orEmpty()
          desiredImports.contains(importPath)
        }
      }
    return if (referencedInFiles.isNotEmpty()) {
      listOf(
        CollectedStat(
          statInfo,
          BooleanStat(
            details = "Referenced in ${referencedInFiles.map { it.name }}",
            value = referencedInFiles.isNotEmpty(),
          )
        )
      )
    } else {
      null
    }
  }

  override fun getName(): String {
    return this::class.java.name
  }
}

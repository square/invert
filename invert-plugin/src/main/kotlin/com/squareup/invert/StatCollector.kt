package com.squareup.invert

import com.squareup.invert.models.Stat
import com.squareup.invert.models.Stat.ClassDefinitionsStat
import com.squareup.invert.models.Stat.HasImportStat
import com.squareup.invert.models.StatInfo
import org.gradle.api.Named
import java.io.File

/**
 * Interface representing all Invert [StatCollector]s.
 *
 * We must extend the [Named] interface to be used as a task input.
 */
sealed interface StatCollector : Named {

  /**
   * Metadata about what data is being collected
   */
  val statInfo: StatInfo

  /**
   * A [StatCollector] that collects [HasImportStat] data.
   */
  interface HasImportStatCollector : StatCollector {
    fun collect(
      kotlinSourceFiles: List<File>
    ): HasImportStat?
  }

  /**
   * A [StatCollector] that collects [DefinitionsCollector] data.
   */
  interface DefinitionsCollector : StatCollector {
    fun collect(
      srcFolder: File,
      projectPath: String,
      kotlinSourceFiles: List<File>
    ): ClassDefinitionsStat?
  }

    /**
    * A [StatCollector] that collects [GenericStatCollector] data.
    */
    interface GenericStatCollector : StatCollector {
        fun collect(
            srcFolder: File,
            projectPath: String,
            kotlinSourceFiles: List<File>
        ): Stat.GenericStat?
    }
}

package com.squareup.invert.internal.report

import com.squareup.invert.internal.models.InvertCombinedCollectedData
import com.squareup.invert.logging.InvertLogger
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatFilter
import com.squareup.invert.models.StatKey
import com.squareup.invert.models.StatMetadata
import com.squareup.invert.models.js.TechDebtInitiative
import com.squareup.invert.models.js.TechDebtInitiative.StatRole
import com.squareup.invert.models.utils.StatFilters

/**
 * Publishes the filtered stats of [TechDebtInitiative]s ([TechDebtInitiative.remainingFilter] and
 * [TechDebtInitiative.completedFilter]) as derived stats.
 *
 * A derived stat is added to every module that has its source stat, so it flows through totals,
 * history, the web report and the per-stat exports exactly like a collected stat.
 */
internal object TechDebtInitiativeStats {

  /** [StatMetadata.category] of derived stats. */
  const val CATEGORY = "Tech Debt Initiatives"

  class Result(
    val collectedData: InvertCombinedCollectedData,
    /** Keys of the derived stats that were added. */
    val derivedStatKeys: Set<StatKey>,
  )

  private class DerivedStat(
    val tdi: TechDebtInitiative,
    val role: StatRole,
    val sourceStatKey: StatKey,
    val filter: StatFilter,
  ) {
    val key: StatKey = filter.key ?: TechDebtInitiative.derivedStatKey(tdi.id, role)
  }

  /**
   * @throws IllegalArgumentException if any filter is invalid, listing every problem found.
   */
  fun addDerivedStats(
    collectedData: InvertCombinedCollectedData,
    techDebtInitiatives: List<TechDebtInitiative>,
    logger: InvertLogger,
  ): Result {
    val errors = mutableListOf<String>()
    val derivedStats = techDebtInitiatives.flatMap { tdi ->
      listOfNotNull(
        tdi.remainingFilter?.let { DerivedStat(tdi, StatRole.REMAINING, tdi.remainingStatKey, it) },
        tdi.completedFilter?.let { filter ->
          val completedStatKey = tdi.completedStatKey
          if (completedStatKey == null) {
            errors.add("TDI '${tdi.id}' has a completedFilter but no completedStatKey.")
            null
          } else {
            DerivedStat(tdi, StatRole.COMPLETED, completedStatKey, filter)
          }
        },
      )
    }
    if (derivedStats.isEmpty()) {
      throwIfAny(errors)
      return Result(collectedData, emptySet())
    }

    val collectedStatInfos: Map<StatKey, StatMetadata> = collectedData.collectedStats
      .flatMap { it.statInfos.values }
      .associateBy { it.key }

    derivedStats.groupBy { it.key }.filterValues { it.size > 1 }.forEach { (key, duplicates) ->
      errors.add(
        "Derived stat key '$key' is produced more than once " +
          "(TDI ids: ${duplicates.joinToString { it.tdi.id }}). " +
          "TDI ids and StatFilter keys must be unique."
      )
    }

    val derivedMetadataByKey = mutableMapOf<StatKey, StatMetadata>()
    val transformsBySourceKey = mutableMapOf<StatKey, MutableList<Pair<StatMetadata, (Stat) -> Stat>>>()
    derivedStats.distinctBy { it.key }.forEach { derivedStat ->
      val tdiId = derivedStat.tdi.id
      val roleName = derivedStat.role.keySuffix
      if (derivedStat.key in collectedStatInfos) {
        errors.add(
          "TDI '$tdiId' derived stat key '${derivedStat.key}' is already used by a collected stat."
        )
        return@forEach
      }
      val sourceMetadata = collectedStatInfos[derivedStat.sourceStatKey]
      if (sourceMetadata == null) {
        logger.warn(
          "TDI '$tdiId': $roleName stat '${derivedStat.sourceStatKey}' was not collected, " +
            "so derived stat '${derivedStat.key}' was not created."
        )
        return@forEach
      }
      val filterErrors = StatFilters.validate(derivedStat.filter, sourceMetadata)
      if (filterErrors.isNotEmpty()) {
        errors.addAll(filterErrors.map { "TDI '$tdiId' ${roleName}Filter: $it" })
        return@forEach
      }

      val derivedMetadata = sourceMetadata.copy(
        key = derivedStat.key,
        title = "${derivedStat.tdi.title} (${derivedStat.role.displayName})",
        description = buildString {
          append(sourceMetadata.description)
          append("\n\nFiltered from `${derivedStat.sourceStatKey}` for Tech Debt Initiative ")
          append("`$tdiId` (${derivedStat.role.displayName}).")
        },
        category = CATEGORY,
      )
      derivedMetadataByKey[derivedStat.key] = derivedMetadata
      transformsBySourceKey.getOrPut(derivedStat.sourceStatKey) { mutableListOf() }
        .add(derivedMetadata to StatFilters.transform(derivedStat.filter))
    }

    throwIfAny(errors)

    val updatedCollectedStats = collectedData.collectedStats.map { project ->
      val addedStatInfos = project.statInfos.keys
        .flatMap { sourceKey -> transformsBySourceKey[sourceKey].orEmpty() }
        .associate { (derivedMetadata, _) -> derivedMetadata.key to derivedMetadata }
      val addedStats = project.stats.entries.flatMap { (sourceKey, stat) ->
        transformsBySourceKey[sourceKey].orEmpty().map { (derivedMetadata, transform) ->
          derivedMetadata.key to transform(stat)
        }
      }.toMap()
      if (addedStatInfos.isEmpty() && addedStats.isEmpty()) {
        project
      } else {
        project.copy(
          statInfos = project.statInfos + addedStatInfos,
          stats = project.stats + addedStats,
        )
      }
    }.toSet()

    logger.info(
      "Added ${derivedMetadataByKey.size} derived Tech Debt Initiative stat(s): " +
        derivedMetadataByKey.keys.sorted().joinToString()
    )
    return Result(
      collectedData = collectedData.copy(collectedStats = updatedCollectedStats),
      derivedStatKeys = derivedMetadataByKey.keys,
    )
  }

  private fun throwIfAny(errors: List<String>) {
    require(errors.isEmpty()) {
      "Invalid Tech Debt Initiative filters:\n" + errors.joinToString("\n") { "  - $it" }
    }
  }
}

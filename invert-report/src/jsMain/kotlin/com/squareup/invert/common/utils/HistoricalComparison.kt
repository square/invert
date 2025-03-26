package com.squareup.invert.common.utils

import com.squareup.invert.models.js.StatTotalAndMetadata
import com.squareup.invert.common.model.RelativeChangeValues
import com.squareup.invert.common.model.StatComparisonResult


object HistoricalComparison {

  fun compareOldestAndNewest(
    oldStats: StatTotalAndMetadata?,
    currentStats: StatTotalAndMetadata?
  ): StatComparisonResult? {

    if (oldStats == null || currentStats == null) {
      return null
    }

    val comparisonResult = compareStats(oldStats, currentStats)
    return comparisonResult
  }


  fun compareStats(old: StatTotalAndMetadata, current: StatTotalAndMetadata): StatComparisonResult {
    val ownerChanges = compareMaps(old.totalByOwner, current.totalByOwner)

    return StatComparisonResult(
      statMetadata = current.metadata,
      currentTotal = current.total,
      previousTotal = old.total,
      relativeTotalChange = current.total - old.total,
      ownerChanges = ownerChanges
    )
  }

  fun compareMaps(
    oldMap: Map<String, Int>,
    currentMap: Map<String, Int>
  ): Map<String, RelativeChangeValues> {
    val allKeys = oldMap.keys + currentMap.keys
    return allKeys.associateWith { key ->
      val oldValue = oldMap[key]
      val currentValue = currentMap[key]
      val relativeChange = if (oldValue != null && currentValue != null && oldValue != 0) {
        (currentValue - oldValue)
      } else {
        null
      }
      RelativeChangeValues(
        oldValue = oldValue ?: 0,
        currentValue = currentValue ?: 0,
        relativeChange = relativeChange ?: 0
      )
    }
  }
}

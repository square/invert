package com.squareup.invert.common.utils

import com.squareup.invert.models.js.MetadataJsReportModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object FormattingUtils {
  fun Int.formatDecimalSeparator(): String {
    return toString()
      .reversed()
      .chunked(3)
      .joinToString(",")
      .reversed()
  }

  internal fun MetadataJsReportModel.dateDisplayStr(): String {
    val instant = Instant.fromEpochSeconds(currentTime)
    val timeZone = TimeZone.currentSystemDefault()
    val dateTime = instant.toLocalDateTime(timeZone)

    // Format the date and time in the desired format
    val formattedDate = "${
      dateTime.month.name.lowercase().replaceFirstChar { it.uppercase() }
    } ${dateTime.dayOfMonth}, ${dateTime.year} at " +
        "${dateTime.hour.toString().padStart(2, '0')}:${
          dateTime.minute.toString().padStart(2, '0')
        }:${dateTime.second.toString().padStart(2, '0')} ($currentTimezoneId)"

    return formattedDate
  }
}
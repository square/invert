package com.squareup.invert.common.utils

import com.squareup.invert.models.js.MetadataJsReportModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.js.Date

object FormattingUtils {
  fun Int.formatDecimalSeparator(): String {
    return if (this < 1000) {
      this.toString()
    } else {
      toString()
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()
    }
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

  /**
   * Formats an epoch time in seconds to a date string in the format "YYYY-MM-DD".
   */
  fun formatEpochToDate(epochSeconds: Long): String {
    val date = Date(epochSeconds * 1000)
    val year = date.getFullYear()
    val month = (date.getMonth() + 1).toString().padStart(2, '0') // Months are 0-indexed
    val day = date.getDate().toString().padStart(2, '0')

    return "$year-$month-$day"
  }
}
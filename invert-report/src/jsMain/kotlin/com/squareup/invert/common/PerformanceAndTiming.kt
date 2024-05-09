package com.squareup.invert.common

import kotlin.time.measureTime

object PerformanceAndTiming {
  suspend fun <T> computeMeasureDuration(
    a: String,
    work: suspend () -> T
  ): T {
    Log.d("--- Starting $a")
    var result: T
    val duration = measureTime {
      result = work()
    }
    Log.d("${duration.inWholeMilliseconds}ms *** Finished $a")
    return result
  }

  fun <T> computeMeasureDurationBlocking(
    a: String,
    work: () -> T
  ): T {
    Log.d("--- Starting $a")
    var result: T
    val duration = measureTime {
      result = work()
    }
    Log.d("${duration.inWholeMilliseconds}ms *** Finished $a")
    return result
  }
}

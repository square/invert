package com.squareup.invert.common

object Log {
  private val DEBUG = false
  fun d(str: String) {
    if (DEBUG) {
      println(str)
    }
  }
}

package com.squareup.invert.common

import kotlinx.browser.window

object Log {
  private val DEBUG = window.location.host.contains("localhost")
  fun d(str: String) {
    if (DEBUG) {
      println(str)
    }
  }
}

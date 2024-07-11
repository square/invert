package com.squareup.invert.logging

object SystemOutInvertLogger : InvertLogger {
  override fun info(message: String) {
    println(message)
  }

  override fun lifecycle(message: String) {
    println(message)
  }

  override fun warn(message: String) {
    println(message)
  }
}
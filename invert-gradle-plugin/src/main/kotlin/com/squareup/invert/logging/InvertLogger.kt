package com.squareup.invert.logging

interface InvertLogger {
  fun info(message: String)
  fun lifecycle(message: String)
  fun warn(message: String)
}
package com.squareup.invert.logging

import org.gradle.api.logging.Logger

class GradleInvertLogger(private val logger: Logger) : InvertLogger {
  override fun info(message: String) {
    logger.info(message)
  }

  override fun lifecycle(message: String) {
    logger.lifecycle(message)
  }

  override fun warn(message: String) {
    logger.warn(message)
  }
}
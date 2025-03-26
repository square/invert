package com.squareup.invert.common.model

data class RelativeChangeValues(
  val oldValue: Int,
  val currentValue: Int,
  val relativeChange: Int,
)

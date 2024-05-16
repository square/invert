package com.squareup.invert.suppress

data class Suppression(
    val type: String,
    val startLine: Int,
    val startColumn: Int,
    val filePath: String?,
)
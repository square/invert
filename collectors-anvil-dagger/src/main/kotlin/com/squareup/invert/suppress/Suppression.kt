package com.squareup.invert.suppress

data class Suppression(
    val type: String,
    val startLine: Int,
    val endLine: Int,
    val filePath: String,
    val code: String? = null,
)
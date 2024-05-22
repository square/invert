package com.squareup.invert.examples.models

import kotlinx.serialization.Serializable

@Serializable
data class Item(
    val label: String,
    val image: String,
    val link: String? = null
)

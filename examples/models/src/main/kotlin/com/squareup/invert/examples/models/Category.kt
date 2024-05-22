package com.squareup.invert.examples.models

@kotlinx.serialization.Serializable
data class Category(val label: String, val image: String, val link: String? = null)

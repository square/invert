package com.squareup.invert

import kotlinx.serialization.Serializable

/**
* Represents the data in an Anvil ContributesBinding Annotation Usage
*/
@Serializable
data class AnvilContributesBinding(
    val annotation: String,
    val scope: String,
    val boundImplementation: String,
    val boundType: String,
    val replaces: List<String>,
    val fileName: String,
)
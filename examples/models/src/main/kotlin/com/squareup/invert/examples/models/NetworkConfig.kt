package com.squareup.invert.examples.models

interface NetworkConfig {
    val baseUrl: String
    val port: Int
    val isMockServer: Boolean
    val fullUrl: String
}

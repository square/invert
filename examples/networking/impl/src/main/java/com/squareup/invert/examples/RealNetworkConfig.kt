package com.squareup.invert.examples

import com.squareup.invert.examples.di.ServerBaseUrl
import com.squareup.invert.examples.models.NetworkConfig
import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.invert.examples.scopes.AppScope
import javax.inject.Inject

@ContributesBinding(AppScope::class)
class RealNetworkConfig @Inject constructor(
    @ServerBaseUrl override val baseUrl: String,
) : NetworkConfig {

    override val port: Int = 443
    override val isMockServer: Boolean = false

    override val fullUrl by lazy {
        var fullUrl = "$baseUrl:$port"
        if (!baseUrl.endsWith("/")) {
            fullUrl += "/"
        }
        fullUrl
    }
}

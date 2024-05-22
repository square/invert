package com.squareup.invert.examples

import com.squareup.invert.examples.LiveNetworkGraph.Companion.configureKtorClient
import com.squareup.invert.examples.di.ServerBaseUrl
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import okhttp3.OkHttpClient
import com.squareup.invert.examples.scopes.AppScope

@ContributesTo(AppScope::class)
@Module
abstract class NetworkDaggerModule {

    companion object {
        @Provides
        fun provideHttpClient(): HttpClient {
            return configureKtorClient(HttpClient(OkHttp) {
                engine {
                    preconfigured = OkHttpClient.Builder().build()
                }
            })
        }

        @Provides
        @ServerBaseUrl
        fun provideBaseUrl(): String {
            return "https://shopping-app.s3.amazonaws.com"
        }
    }
}

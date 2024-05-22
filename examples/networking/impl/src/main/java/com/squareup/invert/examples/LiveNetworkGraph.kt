package com.squareup.invert.examples

import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.invert.examples.di.NetworkGraph
import com.squareup.invert.examples.models.Item
import com.squareup.invert.examples.models.NetworkConfig
import com.squareup.invert.examples.network.Response
import com.squareup.invert.examples.repository.CategoryRepo
import com.squareup.invert.examples.repository.ItemRepo
import com.squareup.invert.examples.repository.UserRepo
import com.squareup.invert.examples.scopes.AppScope
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import kotlinx.serialization.json.Json
import javax.inject.Inject

@ContributesBinding(AppScope::class)
class LiveNetworkGraph @Inject constructor(
    override val userRepo: UserRepo,
    override val categoryRepo: CategoryRepo,
    val networkConfig: NetworkConfig,
    val ktorClient: HttpClient,
) : NetworkGraph {

    override val itemRepo: ItemRepo = object : ItemRepo {
        override suspend fun getItemsForCategory(categoryLabel: String): Response<List<Item>> {
            val itemsForCategoryUrl = "${networkConfig.fullUrl}category/${categoryLabel}/items"
            val response =
                ktorClient.request(itemsForCategoryUrl) {
                    method = HttpMethod.Get
                }

            if (response.status == HttpStatusCode.OK) {
                val responseBody = Json.decodeFromString<List<Item>>(response.bodyAsText())
                return Response.Success(responseBody)
            } else {
                return Response.Failure()
            }
        }
    }

    companion object {
        fun configureKtorClient(ktorClient: HttpClient): HttpClient {
            return ktorClient.config {
                install(Logging) {
                    logger = object : Logger {
                        override fun log(message: String) {
                            println(message)
                        }
                    }
                    level = LogLevel.HEADERS
                }
                install(ContentNegotiation) {
                    val jsonConfig = Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    }
                    val contentTypes = listOf(
                        ContentType.Application.Json,
                        ContentType("binary", "octet-stream"), // S3 Bucket
                    )

                    contentTypes.forEach { contentType ->
                        println("Registering: $contentType")
                        register(contentType, KotlinxSerializationConverter(jsonConfig))
                    }
                }
            }
        }
    }
}

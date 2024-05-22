package com.squareup.invert.examples

import com.squareup.invert.examples.models.Category
import com.squareup.invert.examples.models.NetworkConfig
import com.squareup.invert.examples.network.Response
import com.squareup.invert.examples.repository.CategoryRepo
import com.squareup.anvil.annotations.ContributesBinding
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import com.squareup.invert.examples.scopes.AppScope
import javax.inject.Inject

@ContributesBinding(AppScope::class)
class LiveNetworkCategoryRepo @Inject constructor(
    private val networkConfig: NetworkConfig,
    private val ktorClient: HttpClient,
) : CategoryRepo {
    override suspend fun getCategories(): Response<List<Category>> {
        val response = ktorClient.request("${networkConfig.fullUrl}categories") {
            method = HttpMethod.Get
        }

        if (response.status == HttpStatusCode.OK) {
            println("RESPONSE")
            println(response)
            val json: String = response.bodyAsText()
            println(json)
            val decoded = Json.decodeFromString<List<Category>>(json)
            println(decoded)
            return Response.Success(decoded)
        }
        return Response.Failure()
    }
}

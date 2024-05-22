package com.squareup.invert.examples

import com.squareup.invert.examples.models.LoginRequest
import com.squareup.invert.examples.models.NetworkConfig
import com.squareup.invert.examples.models.User
import com.squareup.invert.examples.network.Response
import com.squareup.invert.examples.repository.UserRepo
import com.squareup.anvil.annotations.ContributesBinding
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import com.squareup.invert.examples.scopes.AppScope
import javax.inject.Inject

@ContributesBinding(AppScope::class)
class LiveNetworkUserRepo @Inject constructor(
    private val networkConfig: NetworkConfig,
    private val ktorClient: HttpClient,
) : UserRepo {

    override suspend fun login(loginRequest: LoginRequest): Response<User> {
        val response = ktorClient.request("${networkConfig.fullUrl}login") {
            method = HttpMethod.Post
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(loginRequest)
        }

        if (response.status == HttpStatusCode.OK) {
            val user = response.body<User>()
            return Response.Success(user)

        }
        return Response.Failure()
    }
}

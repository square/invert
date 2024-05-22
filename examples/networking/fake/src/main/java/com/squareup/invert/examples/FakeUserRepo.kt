package com.squareup.invert.examples

import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.invert.examples.models.LoginRequest
import com.squareup.invert.examples.models.User
import com.squareup.invert.examples.network.Response
import com.squareup.invert.examples.repository.UserRepo
import com.squareup.invert.examples.scopes.AppScope
import javax.inject.Inject

@ContributesBinding(AppScope::class)
class FakeUserRepo @Inject constructor(
    private val mockAccount: MockAccount,
) : UserRepo {
    override suspend fun login(loginRequest: LoginRequest): Response<User> {
        return Response.Success(mockAccount.getUser())
    }
}
package com.squareup.invert.examples.repository

import com.squareup.invert.examples.models.LoginRequest
import com.squareup.invert.examples.models.User
import com.squareup.invert.examples.network.Response

interface UserRepo {
    suspend fun login(loginRequest: LoginRequest): Response<User>
}

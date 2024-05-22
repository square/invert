package com.squareup.invert.examples.repository

import com.squareup.invert.examples.models.Category
import com.squareup.invert.examples.network.Response

interface CategoryRepo {
    suspend fun getCategories(): Response<List<Category>>
}

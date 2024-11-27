package com.squareup.invert.examples

import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.invert.examples.models.Category
import com.squareup.invert.examples.network.Response
import com.squareup.invert.examples.repository.CategoryRepo
import com.squareup.invert.examples.scopes.AppScope
import javax.inject.Inject

@ContributesBinding(AppScope::class)
class FakeCategoryRepo @Inject constructor(
    private val mockAccount: MockAccount
) : CategoryRepo {
    override suspend fun getCategories(
    ): Response<List<Category>> {
        val categories = mockAccount.getCategories()
        return Response.Success(categories)
    }
}

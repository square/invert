package com.squareup.invert.examples

import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.invert.examples.models.Item
import com.squareup.invert.examples.network.Response
import com.squareup.invert.examples.repository.ItemRepo
import com.squareup.invert.examples.scopes.AppScope
import javax.inject.Inject

@ContributesBinding(AppScope::class)
class FakeItemRepo @Inject constructor(
    private val mockAccount: MockAccount
) : ItemRepo {
    override suspend fun getItemsForCategory(categoryLabel: String): Response<List<Item>> {
        val itemsForCategory = mockAccount.getItemsForCategory(categoryLabel)
        if (itemsForCategory != null) {
            return Response.Success(itemsForCategory)
        } else {
            return Response.Failure()
        }
    }
}

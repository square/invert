package com.squareup.invert.examples

import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.invert.examples.di.NetworkGraph
import com.squareup.invert.examples.models.Category
import com.squareup.invert.examples.models.Item
import com.squareup.invert.examples.models.LoginRequest
import com.squareup.invert.examples.models.User
import com.squareup.invert.examples.network.Response
import com.squareup.invert.examples.repository.CategoryRepo
import com.squareup.invert.examples.repository.ItemRepo
import com.squareup.invert.examples.repository.UserRepo
import com.squareup.invert.examples.scopes.AppScope
import javax.inject.Inject

@ContributesBinding(AppScope::class)
class FakeNetworkGraph @Inject constructor() : NetworkGraph {

    private val mockAccount: MockAccount = ProduceMockAccount()

    override val categoryRepo: CategoryRepo =
        object : CategoryRepo {
            override suspend fun getCategories(): Response<List<Category>> {
                val categories = mockAccount.getCategories()
                return Response.Success(categories)
            }
        }

    override val itemRepo: ItemRepo =
        object : ItemRepo {
            override suspend fun getItemsForCategory(categoryLabel: String): Response<List<Item>> {
                val itemsForCategory = mockAccount.getItemsForCategory(categoryLabel)
                if (itemsForCategory != null) {
                    return Response.Success(itemsForCategory)
                } else {
                    return Response.Failure()
                }
            }
        }

    override val userRepo: UserRepo =
        object : UserRepo {
            override suspend fun login(loginRequest: LoginRequest): Response<User> {
                return Response.Success(mockAccount.getUser())
            }
        }
}

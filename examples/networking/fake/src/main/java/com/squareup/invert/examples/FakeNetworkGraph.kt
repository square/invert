package com.squareup.invert.examples

import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.invert.examples.di.NetworkGraph
import com.squareup.invert.examples.repository.CategoryRepo
import com.squareup.invert.examples.repository.ItemRepo
import com.squareup.invert.examples.repository.UserRepo
import com.squareup.invert.examples.scopes.AppScope
import javax.inject.Inject

@ContributesBinding(AppScope::class)
class FakeNetworkGraph @Inject constructor(
    override val categoryRepo: CategoryRepo,
    override val itemRepo: ItemRepo,
    override val userRepo: UserRepo
) : NetworkGraph
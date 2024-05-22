package com.squareup.invert.examples.di

import com.squareup.invert.examples.repository.CategoryRepo
import com.squareup.invert.examples.repository.ItemRepo
import com.squareup.invert.examples.repository.UserRepo

interface NetworkGraph {
    val categoryRepo: CategoryRepo
    val itemRepo: ItemRepo
    val userRepo: UserRepo
}

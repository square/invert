package com.squareup.invert.examples


import com.squareup.invert.examples.models.Category
import com.squareup.invert.examples.models.Item
import com.squareup.invert.examples.models.User

interface MockAccount {

    fun getUsername(): String

    fun getUser(): User

    fun getCategories(): List<Category>

    fun getItemsForCategory(categoryLabel: String): List<Item>? {
        return listOf()
    }
}

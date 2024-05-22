package com.squareup.invert.examples


import com.squareup.invert.examples.models.Category
import com.squareup.invert.examples.models.Item
import com.squareup.invert.examples.models.User

abstract class MockAccount {

    val itemsByCategory: MutableMap<String, List<Item>?> = mutableMapOf()

    abstract fun getUsername(): String

    abstract fun getUser(): User

    abstract fun getCategories(): List<Category>

    open fun getItemsForCategory(categoryLabel: String): List<Item>? {
        return listOf()
    }
}

package com.squareup.invert.examples.repository

import com.squareup.invert.examples.models.Item
import com.squareup.invert.examples.network.Response

interface ItemRepo {
    suspend fun getItemsForCategory(categoryLabel: String): Response<List<Item>>
}

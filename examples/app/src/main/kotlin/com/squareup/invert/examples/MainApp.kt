package com.squareup.invert.examples

import com.squareup.invert.examples.network.Response
import kotlinx.coroutines.runBlocking

object MainApp {
    @Suppress("UseCheckOrError")
    @JvmStatic
    fun main(args: Array<String>) {
        val appComponent = DaggerAppComponent.create()

        val networking = appComponent.networking()
        runBlocking {
            when (val categoriesResult = networking.categoryRepo.getCategories()) {
                is Response.Success -> {
                    println("We got categories! ${categoriesResult.body}")
                }

                is Response.Failure -> {
                    throw IllegalStateException("Network Failure $categoriesResult")
                }
            }
        }
        println("The End")
    }
}

@file:Suppress("NewLineAtEndOfFile", "MagicNumber")

package com

import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.CoroutineScope
import scopes.AppScope
import javax.inject.Inject

interface Analytics

@Suppress("SomeRule")
@ContributesBinding(AppScope::class)
class RealAnalytics @Inject constructor(
    @AppCoroutineScope coroutineScope: CoroutineScope,
) : Analytics {
    init {
        for (i in 0..11) {
            while (i < 5) {
                println(i)
            }
        }
    }

    @Suppress("SomeRule")
    fun amazing(): String {
        return "abcd".length.toString()
    }
}

@Suppress("SomeRule")
data class SomeDataClass(val str: String)
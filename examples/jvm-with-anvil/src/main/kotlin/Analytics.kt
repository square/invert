@file:Suppress("NewLineAtEndOfFile", "MagicNumber")

import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.anvil.annotations.ContributesTo
import javax.inject.Inject

interface Analytics

@Suppress("SomeRule")
@ContributesBinding(AppScope::class)
class RealAnalytics @Inject constructor() : Analytics {
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
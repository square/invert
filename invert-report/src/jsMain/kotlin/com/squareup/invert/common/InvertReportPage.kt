package com.squareup.invert.common


import androidx.compose.runtime.Composable
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRoute
import kotlin.reflect.KClass
import kotlin.reflect.cast


/**
 * Represents any page that can be displayed in an Invert Report.
 */
interface InvertReportPage<T : NavRoute> {

    val navPage: NavPage

    val showInNav: Boolean get() = true

    val navRouteKClass: KClass<T>

    val composableContent: @Composable (T) -> Unit

    /**
     * This is how we were able to get Generics to work.  It might not be the correct way.
     */
    @Composable
    fun composableContentWithRouteCast(it: NavRoute) {
        composableContent(navRouteKClass.cast(it))
    }
}

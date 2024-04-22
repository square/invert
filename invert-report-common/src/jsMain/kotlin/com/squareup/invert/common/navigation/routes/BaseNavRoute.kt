package com.squareup.invert.common.navigation.routes

import com.squareup.invert.common.navigation.NavPageId
import com.squareup.invert.common.navigation.NavRoute

abstract class BaseNavRoute(
    override val page: NavPageId,
) : NavRoute {
    override fun toSearchParams(): Map<String, String> = toParamsWithOnlyPageId(this)

    companion object {

        const val PAGE_ID_PARAM = "page"

        fun toParamsWithOnlyPageId(navRoute: NavRoute): MutableMap<String, String> {
            return mutableMapOf<String, String>().apply {
                this[PAGE_ID_PARAM] = navRoute.page
            }
        }
    }
}
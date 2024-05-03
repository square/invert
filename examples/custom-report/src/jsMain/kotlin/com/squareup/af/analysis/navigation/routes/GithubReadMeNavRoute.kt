package com.squareup.af.analysis.navigation.routes

import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.common.navigation.routes.HomeNavRoute

//data class GithubReadMeNavRoute(val orgSlashRepo: String? = null) : BaseNavRoute(NavPage(
//    pageId = "github_readme",
//    displayName = "ReadMe on GitHub",
//    navIconSlug = "file-earmark-bar-graph",
//    navRouteParser = {
//        val repo = it[REPO_KEY]
//        if (repo != null) {
//            GithubReadMeNavRoute(repo)
//        } else {
//            HomeNavRoute
//        }
//    }
//)) {
//    override fun toSearchParams(): Map<String, String> {
//        val searchParams = super.toSearchParams().toMutableMap()
//        orgSlashRepo?.let {
//            searchParams[REPO_KEY] = it
//        }
//        return searchParams
//    }
//
//    companion object {
//        val REPO_KEY = "repo"
//    }
//}
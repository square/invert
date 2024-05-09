package com.squareup.invert.common.navigation.routes

import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.models.ConfigurationName
import com.squareup.invert.models.GradlePath

data class DependencyDiffNavRoute(
    val moduleA: GradlePath? = null,
    val moduleB: GradlePath? = null,
    val configurationA: ConfigurationName? = null,
    val configurationB: ConfigurationName? = null,
    val includeArtifacts: Boolean = false,
    val showMatching: Boolean = false,
) : BaseNavRoute(NavPage(
    pageId = "dependency_diff",
    displayName = "Dependency Diff",
    navIconSlug = "folder",
    navRouteParser = { parser(it) }
)) {

    override fun toSearchParams() = toParamsWithOnlyPageId(this).also { map ->
        moduleA?.let {
            map[MODULE_A_PARAM] = it
        }
        moduleB?.let {
            map[MODULE_B_PARAM] = it
        }
        configurationA?.let {
            map[CONFIGURATION_A_PARAM] = it
        }
        configurationB?.let {
            map[CONFIGURATION_B_PARAM] = it
        }
        map[INCLUDE_ARTIFACTS_PARAM] = includeArtifacts.toString()
        map[SHOW_MATCHING_PARAM] = showMatching.toString()
    }

    companion object {

        private const val MODULE_A_PARAM = "module_a"
        private const val MODULE_B_PARAM = "module_b"
        private const val CONFIGURATION_A_PARAM = "configuration_a"
        private const val CONFIGURATION_B_PARAM = "configuration_b"
        private const val INCLUDE_ARTIFACTS_PARAM = "include_artifacts"
        private const val SHOW_MATCHING_PARAM = "show_matching"

        fun parser(params: Map<String, String?>): NavRoute {
            val moduleA = params[MODULE_A_PARAM]
            val moduleB = params[MODULE_B_PARAM]
            val configurationA = params[CONFIGURATION_A_PARAM]
            val configurationB = params[CONFIGURATION_B_PARAM]
            val includeArtifacts = params[INCLUDE_ARTIFACTS_PARAM].toBoolean()
            val showMatching = params[SHOW_MATCHING_PARAM].toBoolean()
            return DependencyDiffNavRoute(
                moduleA = moduleA,
                moduleB = moduleB,
                configurationA = configurationA,
                configurationB = configurationB,
                includeArtifacts = includeArtifacts,
                showMatching = showMatching,
            )
        }
    }
}

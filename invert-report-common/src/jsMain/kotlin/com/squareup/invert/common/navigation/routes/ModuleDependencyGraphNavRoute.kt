package com.squareup.invert.common.navigation.routes

import com.squareup.invert.common.navigation.NavPage

data class ModuleDependencyGraphNavRoute(
    val module: String? = null,
    val configuration: String? = null,
) : BaseNavRoute(NavPage(
            pageId = "module_dependency_graph",
            displayName = "Module Dependency Graph",
            navIconSlug = "diagram-3",
            navRouteParser = { parser(it) }
            )) {

    override fun toSearchParams(): Map<String, String> = toParamsWithOnlyPageId(this)
        .also { params ->
            module?.let {
                params[MODULE_PARAM] = module
            }
            configuration?.let {
                params[CONFIGURATION_PARAM] = configuration
            }
        }

    companion object {

        private const val MODULE_PARAM = "module"
        private const val CONFIGURATION_PARAM = "configuration"

        fun parser(params: Map<String, String?>): ModuleDependencyGraphNavRoute {
            return ModuleDependencyGraphNavRoute(
                module = params[MODULE_PARAM],
                configuration = params[CONFIGURATION_PARAM]
            )
        }
    }
}

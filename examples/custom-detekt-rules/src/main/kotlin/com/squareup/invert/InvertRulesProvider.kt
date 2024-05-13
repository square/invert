package com.squareup.invert

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class InvertRulesProvider : RuleSetProvider {
    override val ruleSetId: String
        get() = this::class.java.simpleName

    override fun instance(config: Config): RuleSet {
        return RuleSet(
            id = ruleSetId,
            rules = listOf(
                SamsRule(config)
            )
        )
    }
}
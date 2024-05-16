package com

import com.squareup.anvil.annotations.ContributesMultibinding
import scopes.AppScope
import javax.inject.Inject

interface FeatureTogglesPlugin {
    /**
     * This method will return whether the plugin knows about the [featureName] in which case will
     * return whether it is enabled or disabled
     * @return `true` if the feature is enable. `false` when disabled. `null` if the plugin does not
     * know the featureName. [defaultValue] if the plugin knows featureName but is not set
     */
    fun isEnabled(
        featureName: String,
        defaultValue: Boolean,
    ): Boolean?
}


@ContributesMultibinding(AppScope::class)
class AdClickFeatureTogglesPlugin @Inject constructor(
    private val analytics: Analytics,
    private val authenticator: Authenticator,
) : FeatureTogglesPlugin {
    override fun isEnabled(featureName: String, defaultValue: Boolean): Boolean? {
        return true
    }
}

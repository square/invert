package com.squareup.af.analysis.stats

import com.squareup.af.analysis.models.KtClassInfo

object KtClassInfoMatchesExpectedType {
    fun ktClassInfoKeywordMatcher(
        actual: KtClassInfoKeywordData,
        isInterface: Boolean,
        isEnum: Boolean,
        isAbstract: Boolean,
    ): Boolean {
        return (actual.isInterface == isInterface)
                && (actual.isEnum == isEnum)
                && (actual.isAbstract == isAbstract)
    }
}

typealias KtClassInfoKeywordMatcher = (ktClassInfo: KtClassInfoKeywordData) -> Boolean

data class KtClassInfoKeywordData(
    val isInterface: Boolean,
    val isEnum: Boolean,
    val isAbstract: Boolean,
)
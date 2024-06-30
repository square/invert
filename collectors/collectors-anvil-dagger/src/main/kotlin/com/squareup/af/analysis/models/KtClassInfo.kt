package com.squareup.af.analysis.models

import com.squareup.af.analysis.stats.KtClassInfoKeywordData
import org.jetbrains.kotlin.psi.KtClass

/**
 * Abstraction layer on top of [KtClass] to allow better logic testing with fakes.
 *
 * Feel free to add as many properties as are needed as delegates.
 */
interface KtClassInfo {
    fun isInterface(): Boolean
    fun isEnum(): Boolean
    fun isAbstract(): Boolean

    companion object {
        fun KtClass.toKtClassInfo(): KtClassInfo = RealKtClassInfo(this)
        fun KtClass.toKtClassInfoData(): KtClassInfoKeywordData= RealKtClassInfo(this).run {
            KtClassInfoKeywordData(
                isEnum = isEnum(),
                isAbstract = isAbstract(),
                isInterface = isInterface(),
            )
        }
    }
}

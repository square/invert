package com.squareup.af.analysis.models

import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.isAbstract

/**
 * Delegates to a concrete implementation of [KtClass]
 */
class RealKtClassInfo(private val ktClass: KtClass) : KtClassInfo {
  override fun isInterface(): Boolean = ktClass.isInterface()
  override fun isEnum(): Boolean = ktClass.isEnum()
  override fun isAbstract(): Boolean = ktClass.isAbstract()
}

package com.squareup.psi

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles.JVM_CONFIG_FILES
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.extensions.ExtensionPoint
import org.jetbrains.kotlin.com.intellij.openapi.util.UserDataHolderBase
import org.jetbrains.kotlin.com.intellij.pom.PomModel
import org.jetbrains.kotlin.com.intellij.pom.PomModelAspect
import org.jetbrains.kotlin.com.intellij.pom.PomTransaction
import org.jetbrains.kotlin.com.intellij.pom.impl.PomTransactionBase
import org.jetbrains.kotlin.com.intellij.pom.tree.TreeAspect
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.TreeCopyHandler
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.lexer.KtTokens
import sun.reflect.ReflectionFactory
import kotlin.LazyThreadSafetyMode.NONE

internal val PSI_FILE_FACTORY by lazy(NONE) {
    val project = KotlinCoreEnvironment.createForProduction(
        projectDisposable = {},
        configuration = CompilerConfiguration().apply {
            put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        },
        configFiles = JVM_CONFIG_FILES
    ).project as MockProject

    project.enableASTMutations()

    PsiFileFactory.getInstance(project)
}

// This is what KtLint is doing to enable code formatting.
private fun MockProject.enableASTMutations() {
    val extensionPoint = "org.jetbrains.kotlin.com.intellij.treeCopyHandler"
    val extensionClassName = TreeCopyHandler::class.java.name
    if (!extensionArea.hasExtensionPoint(extensionPoint)) {
        extensionArea.registerExtensionPoint(
            extensionPoint,
            extensionClassName,
            ExtensionPoint.Kind.INTERFACE,
            false
        )
    }

    registerService(PomModel::class.java, FormatPomModel())
}

private class FormatPomModel : UserDataHolderBase(), PomModel {

    override fun runTransaction(
        transaction: PomTransaction
    ) = (transaction as PomTransactionBase).run()

    @Suppress("UNCHECKED_CAST")
    override fun <T : PomModelAspect> getModelAspect(
        aspect: Class<T>
    ): T? {
        return if (aspect == TreeAspect::class.java) {
            // using approach described in https://git.io/vKQTo due to the magical bytecode of TreeAspect
            // (check constructor signature and compare it to the source)
            // (org.jetbrains.kotlin:kotlin-compiler-embeddable:1.0.3)
            val constructor = ReflectionFactory
                .getReflectionFactory()
                .newConstructorForSerialization(aspect, Any::class.java.getDeclaredConstructor())
            constructor.newInstance() as T
        } else {
            null
        }
    }
}

fun PsiElement.deleteFromParentWithWhitespace() {
    val whiteSpace = nextSibling?.let {
        if (it is PsiWhiteSpace) it else null
    }

    // Sometimes there's an extra white space. Remove it when needed.
    parent.deleteChildRange(this, whiteSpace ?: this)
}

/**
* Deletes an element from a list with multiple arguments, e.g. a function call, constructor, etc.
*
* ```
* @Annotation(elem1, elem2) -> @Annotation(elem2)
*
* function(a, b, c) -> function(a, c)
* ```
*/
fun PsiElement.deleteFromParentWithComma() {
    // Try to find the comma before this element.
    val prevCommaElement = prevSibling
        .takeIf { it.isWhiteSpace() }
        ?.prevSibling
        ?.takeIf { it.isComma() }

    // If it's the first element in the list, then try to find the comma after this element.
    val nextCommaElement = nextSibling
        .takeIf { it.isComma() }
        ?.nextSibling
        ?.takeIf { it.isWhiteSpace() }

    when {
        prevCommaElement != null -> parent.deleteChildRange(prevCommaElement, this)
        nextCommaElement != null -> parent.deleteChildRange(this, nextCommaElement)
        // Delete all unnecessary whitespaces, too.
        else -> parent.deleteChildRange(
            prevSiblings().lastOrNull { it.isWhiteSpace() } ?: this,
            nextSiblings().lastOrNull { it.isWhiteSpace() } ?: this
        )
    }
}

private fun PsiElement.isWhiteSpace(): Boolean = this is PsiWhiteSpace
private fun PsiElement.isComma(): Boolean = this is LeafPsiElement && elementType == KtTokens.COMMA

private fun PsiElement.prevSiblings(): Sequence<PsiElement> =
    generateSequence(prevSibling) { it.prevSibling }
private fun PsiElement.nextSiblings(): Sequence<PsiElement> =
    generateSequence(nextSibling) { it.nextSibling }

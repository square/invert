package com.squareup.invert

import io.gitlab.arturbosch.detekt.api.DetektVisitor
import io.gitlab.arturbosch.detekt.api.FileProcessListener
import io.gitlab.arturbosch.detekt.api.Finding
import org.jetbrains.kotlin.com.intellij.openapi.util.Key
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext

@Suppress("WHOA")
class SamsFileProcessListener : FileProcessListener {

    override fun onProcess(file: KtFile, bindingContext: BindingContext) {
        super.onProcess(file, bindingContext)
        val visitor = LoopVisitor()
        file.accept(visitor)
        file.putUserData(numberOfLoopsKey, visitor.numberOfLoops)
        file.putUserData(suppressedKey, visitor.suppressed)
    }

    override fun onProcessComplete(file: KtFile, findings: Map<String, List<Finding>>, bindingContext: BindingContext) {
        super.onProcessComplete(file, findings, bindingContext)
//        println("onProcessComplete-numberOfLoopsKey" + file.getUserData(numberOfLoopsKey).toString())
        println(
            "onProcessComplete-AllSuppress ${file.containingFile.name} "
        )
        println(file.getUserData(suppressedKey).toString())
    }

    companion object {
        val numberOfLoopsKey = Key<Int>("number of loops")
        val suppressedKey = Key<List<String>>("AllSuppress")

        fun printTree(ktElement: PsiElement, indent: String = ""): String {
            val isRoot = indent.isEmpty()
            return buildString {
                if (isRoot) {
                    appendLine("-- $ktElement in ${ktElement.containingFile.name} ---")
                }
                appendLine("KtElement Class: " + ktElement::class.java.simpleName)
                if (ktElement is KtTypeElement) {
                    appendLine("FqName: ${ktElement.requireFqName()} ")
                }
                appendLine("Text (${ktElement.textRange.startOffset}:${ktElement.textRange.endOffset}) " + ktElement.text)
                ktElement.children.forEach {
                    appendLine(printTree(it, "$indent  "))
                }
            }.lines().filter { it.trim().isNotBlank() }.joinToString("\n") { indent + it }.also {
                if (isRoot) {
                    println(it)
                }
            }
        }
    }

    class LoopVisitor : DetektVisitor() {

        internal var numberOfLoops = 0
        internal var suppressed = mutableListOf<String>()
        override fun visitLoopExpression(loopExpression: KtLoopExpression) {
            super.visitLoopExpression(loopExpression)
            numberOfLoops++
        }

        data class AnnotationData(
            val fqName: String,
            val args: List<String>,
            val text: String,
        )


        override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
            super.visitAnnotationEntry(annotationEntry)

            val annotationClassType = PsiTreeUtil.findChildOfType(annotationEntry, KtTypeReference::class.java)

            val annotationArgumentList: List<String> =
                PsiTreeUtil.findChildOfType(
                    annotationEntry,
                    KtValueArgumentList::class.java
                )?.children?.filterIsInstance<KtValueArgument>()
                    ?.map { it.text ?: "UNKNOWN" }
                    ?: listOf()

            val annotationFqName = annotationClassType?.requireFqName()
                ?.asString()

            annotationFqName?.let {
                if (it.endsWith("Suppress")) {
                    val annotationData = AnnotationData(
                        fqName = annotationClassType.requireFqName().asString(),
                        args = annotationArgumentList,
                        text = annotationEntry.text
                    )
//            printTree(annotationEntry)
                    suppressed.add(annotationData.toString())
                }
            }

        }
    }
}
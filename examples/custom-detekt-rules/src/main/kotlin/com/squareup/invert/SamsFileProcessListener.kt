package com.squareup.invert

import io.github.detekt.psi.toFilePath
import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.com.intellij.openapi.util.Key
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext

fun PsiElement.printTree(indent: String = ""): String {
    val ktElement = this
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
            appendLine(it.printTree("$indent  "))
        }
    }.lines().filter { it.trim().isNotBlank() }.joinToString("\n") { indent + it }.also {
        if (isRoot) {
            println(it)
        }
    }
}

@Suppress("WHOA")
class SamsFileProcessListener : FileProcessListener {

    data class Suppression(
        val type: String,
        val startOffset: Int,
        val filePath: String?,
    )

    override fun onProcess(file: KtFile, bindingContext: BindingContext) {
        super.onProcess(file, bindingContext)
        val visitor = SuppressionsVisitor()
        file.accept(visitor)
        file.putUserData(suppressionsKey, visitor.suppressed)
    }

    override fun onProcessComplete(file: KtFile, findings: Map<String, List<Finding>>, bindingContext: BindingContext) {
        super.onProcessComplete(file, findings, bindingContext)
        println(
            "onProcessComplete-Suppressions ${file.containingFile.name} "
        )
        val suppressions: List<Suppression> = file.getUserData(suppressionsKey) ?: emptyList()
        println("onProcessComplete-Suppressions $suppressions")
    }

    override fun onFinish(files: List<KtFile>, result: Detektion, bindingContext: BindingContext) {
        super.onFinish(files, result, bindingContext)
        println("::onFinish()")

        val allSuppressions = mutableListOf<Suppression>()
        files.forEach { file ->
            val suppressions =
                file.getUserData(suppressionsKey)?.map { it.copy(filePath = file.virtualFilePath) }
                    ?: emptyList()
            allSuppressions.addAll(suppressions)
        }

        result.addData(suppressionsKey, allSuppressions)

        result.add(
            ProjectMetric(
                type = "Suppressions",
                value = allSuppressions.size,
            )
        )
    }

    companion object {
        val numberOfLoopsKey = Key<Int>("number of loops")

        val suppressionsKey = Key<List<Suppression>>("Suppressions")


    }

    class SuppressionsVisitor : DetektVisitor() {

        internal var suppressed = mutableListOf<Suppression>()

        data class AnnotationData(
            val fqName: String,
            val args: List<String>,
            val text: String,
        )

        override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
            super.visitAnnotationEntry(annotationEntry)

            val annotationClassType = PsiTreeUtil.findChildOfType(annotationEntry, KtTypeReference::class.java)
            annotationEntry.getChildOfType<KtTypeReference>()
            annotationEntry.getChildrenOfType<KtTypeReference>()

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
                    annotationData.args.forEach { ignoredType ->
                        suppressed.add(
                            Suppression(
                                type = ignoredType,
                                startOffset = annotationEntry.startOffset,
                                filePath = null
                            )
                        )
                    }
                }
            }

        }
    }
}
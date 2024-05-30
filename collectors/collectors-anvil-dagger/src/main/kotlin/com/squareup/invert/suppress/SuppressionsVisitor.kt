package com.squareup.invert.suppress

import com.squareup.psi.requireFqName
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils.getLineAndColumnRangeInPsiFile
import org.jetbrains.kotlin.psi.*

class SuppressionsVisitor {

    val suppressed = mutableListOf<Suppression>()

    data class AnnotationData(
        val fqName: String,
        val args: List<String>,
        val text: String,
    )

    private fun addNewSuppression(newSuppression: Suppression) = synchronized(suppressed) {
        suppressed.add(newSuppression)
    }

    fun processAnnotationEntry(ktFile: KtFile, annotationEntry: KtAnnotationEntry, fileName: String) {

        val annotationClassType = PsiTreeUtil.findChildOfType(annotationEntry, KtTypeReference::class.java)

        val annotationArgumentList: List<String> =
            PsiTreeUtil.findChildOfType(
                annotationEntry,
                KtValueArgumentList::class.java
            )?.children?.filterIsInstance<KtValueArgument>()
                ?.map { it.text.replace("\"", "") ?: "UNKNOWN" }
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


                val lineAndColumnRange = getLineAndColumnRangeInPsiFile(ktFile, annotationEntry.textRange)


                annotationData.args.forEach { ignoredType ->
                    addNewSuppression(
                        Suppression(
                            type = ignoredType,
                            startLine = lineAndColumnRange.start.line,
                            endLine = lineAndColumnRange.end.line,
                            filePath = fileName,
                            code = annotationData.text
                        )
                    )
                }
            }
        }

    }
}
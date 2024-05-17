package com.squareup.invert

import com.rickbusarow.statik.InternalStatikApi
import com.squareup.invert.models.Stat.ProvidesAndInjectsStat.*
import com.squareup.psi.classesAndInnerClasses
import com.squareup.psi.requireFqName
import com.squareup.psi.toKtFile
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils.getLineAndColumnRangeInPsiFile
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import java.io.File

/**
 * Utility Class responsible for collecting all Anvil @ContributesBinding annotations
 * on classes in a Kotlin file.
 */
class FindAnvilContributesBinding {

    private val allBindings = mutableListOf<DiContribution>()

    private val contributionAndConsumption = mutableListOf<ProvidesAndInjects>()

    fun getCollectedContributionsAndConsumptions(): List<ProvidesAndInjects> {
        return contributionAndConsumption
    }

    fun getCollectedContributesBindings(): List<DiContribution> {
        return allBindings
    }

    private fun KtAnnotationEntry.extractContributesBindingAnnotationArgs(): List<AnnotationArg> {
        val ktAnnotationEntry = this
        val annotationArgs = mutableListOf<AnnotationArg>()
        ktAnnotationEntry.valueArgumentList?.arguments?.forEach { ktValueArgument: KtValueArgument ->
            val argumentName: String? = ktValueArgument.getArgumentName()
                ?.asName
                ?.asString()

            val argumentExpression = ktValueArgument.getArgumentExpression()
            if (argumentExpression is KtClassLiteralExpression) {
                val receiverExpression = argumentExpression.receiverExpression
                if (receiverExpression is KtNameReferenceExpression) {
                    val argumentValue = receiverExpression
                        .requireFqName()
                        .asString()
                    annotationArgs.add(
                        AnnotationArg.AnnotationArgSingle(
                            name = argumentName,
                            value = argumentValue
                        )
                    )
                }
            } else if (argumentExpression is KtCollectionLiteralExpression) {
                val values =
                    argumentExpression.innerExpressions.mapNotNull { subArgumentExpression: KtExpression ->
                        if (subArgumentExpression is KtClassLiteralExpression) {
                            val receiverExpression = subArgumentExpression.receiverExpression
                            if (receiverExpression is KtNameReferenceExpression) {
                                val argumentValue = receiverExpression
                                    .requireFqName()
                                    .asString()
                                argumentValue
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    }
                annotationArgs.add(
                    AnnotationArg.AnnotationArgArray(
                        name = argumentName,
                        values = values
                    )
                )
            }
        }
        return annotationArgs
    }

    fun KtClassOrObject.extractAnnotationInfos(): List<AnnotationInfo> {
        val ktClassOrObject = this
        val annotationInfos = mutableListOf<AnnotationInfo>()
        ktClassOrObject.annotationEntries.forEach { ktAnnotationEntry: KtAnnotationEntry ->
            val annotationFqClassName = ktAnnotationEntry.requireFqName().asString()
            val annotationInfo = when (annotationFqClassName) {
                ANVIL_CONTRIBUTES_BINDING_ANNOTATION_CLASS_NAME,
                    // TODO Do we need to note when it is a multibinding?
                ANVIL_CONTRIBUTES_MULTIBINDING_ANNOTATION_CLASS_NAME -> AnnotationInfo(
                    type = annotationFqClassName,
                    args = ktAnnotationEntry.extractContributesBindingAnnotationArgs()
                )

                else -> AnnotationInfo(
                    type = annotationFqClassName,
                    args = null
                )
            }
            annotationInfo.let {
                annotationInfos.add(it)
            }
        }
        return annotationInfos
    }

    fun handleKotlinFile(file: File, relativeFilePath: String) {
        val bindingsInFile = mutableListOf<DiContribution>()
        if (file.exists()) {
            val ktFile = file.toKtFile()

            ktFile.accept(object : KtTreeVisitorVoid() {
                override fun visitConstructorDelegationCall(call: KtConstructorDelegationCall) {
                    super.visitConstructorDelegationCall(call)
                }
            })

            ktFile.classesAndInnerClasses()
                .toList()
                .forEach { ktClassOrObject ->
                    val bindingsInClassOrObject = mutableListOf<DiContribution>()
                    val classOrObjectFqName = ktClassOrObject.fqName?.asString() ?: ktClassOrObject.name!!

                    val supertypes = ktClassOrObject.getSuperTypeList()?.entries
                        ?.map { it.requireFqName().asString() }
                    val annotationInfos = ktClassOrObject.extractAnnotationInfos()
                    val contributeBindingAnnotationInfo: AnnotationInfo? = annotationInfos
                        .firstOrNull {
                            it.type == ANVIL_CONTRIBUTES_BINDING_ANNOTATION_CLASS_NAME
                                    || it.type == ANVIL_CONTRIBUTES_MULTIBINDING_ANNOTATION_CLASS_NAME
                        }

                    contributeBindingAnnotationInfo?.let {
                        val scope = contributeBindingAnnotationInfo.args?.firstOrNull { it.name == "scope" }
                            ?: contributeBindingAnnotationInfo.args!!.first()

                        val boundType =
                            contributeBindingAnnotationInfo.args?.firstOrNull { it.name == "boundType" }
                        val replaces =
                            contributeBindingAnnotationInfo.args?.firstOrNull { it.name == "replaces" }

                        val anvilContributesBinding = DiContribution(
                            annotation = contributeBindingAnnotationInfo.type,
                            scope = (scope as AnnotationArg.AnnotationArgSingle).value,
                            boundImplementation = classOrObjectFqName,
                            boundType = if (boundType != null && boundType is AnnotationArg.AnnotationArgSingle) {
                                boundType.value
                            } else {
                                null
                            } ?: supertypes?.first()!!,
                            replaces = if (replaces != null && replaces is AnnotationArg.AnnotationArgArray) {
                                replaces.values
                            } else {
                                listOf()
                            },
                        )
                        bindingsInClassOrObject.add(anvilContributesBinding)
                    }

                    //Inject Constructor
                    val consumptions = findConstructorInjections(ktFile, ktClassOrObject)

                    val lineAndColumnRange = getLineAndColumnRangeInPsiFile(ktFile, ktClassOrObject.textRange)
                    if (bindingsInClassOrObject.isNotEmpty() || consumptions.isNotEmpty()) {
                        contributionAndConsumption.add(
                            ProvidesAndInjects(
                                filePath = relativeFilePath,
                                contributions = bindingsInClassOrObject,
                                consumptions = consumptions,
                                startLine = lineAndColumnRange.start.line,
                                endLine = lineAndColumnRange.end.line,
                                classFqName = classOrObjectFqName,
                            )
                        )
                    }
                    bindingsInFile.addAll(bindingsInClassOrObject)
                }
        }
        allBindings.addAll(bindingsInFile)
    }

    fun KtElement.getAnnotationsFromModifierList(): List<String> {
        return getChildOfType<KtDeclarationModifierList>()?.getChildrenOfType<KtAnnotationEntry>()
            ?.map { annotationEntry ->
                annotationEntry.text
            } ?: emptyList()
    }

    @OptIn(InternalStatikApi::class)
    private fun findConstructorInjections(ktFile: KtFile, ktClassOrObject: KtClassOrObject): List<DiInjection> {
        val anvilInjections = mutableListOf<DiInjection>()
        ktClassOrObject.primaryConstructor?.let { primaryConstructor ->
            val hasInject = primaryConstructor.getAnnotationsFromModifierList().any { it.endsWith("Inject") }
            if (hasInject) {
                val lineAndColumnRange = getLineAndColumnRangeInPsiFile(ktFile, primaryConstructor.textRange)
                val ktParameterList = primaryConstructor.getChildOfType<KtParameterList>()!!
                ktParameterList.forEachDescendantOfType<KtParameter> { ktParameter ->

                    val qualifierAnnotations = ktParameter.getAnnotationsFromModifierList()
                    ktParameter.getChildOfType<KtTypeReference>().also { ktTypeReference ->
                        val paramFqName = ktTypeReference!!.requireFqName().asString()
                        anvilInjections.add(
                            DiInjection(
                                type = paramFqName,
                                qualifierAnnotations = qualifierAnnotations,
                                startLine = lineAndColumnRange.start.line,
                                endLine = lineAndColumnRange.end.line,
                            )
                        )
                    }
                }
            }
        }
        return anvilInjections
    }

    companion object {
        const val ANVIL_CONTRIBUTES_BINDING_ANNOTATION_CLASS_NAME =
            "com.squareup.anvil.annotations.ContributesBinding"
        const val ANVIL_CONTRIBUTES_MULTIBINDING_ANNOTATION_CLASS_NAME =
            "com.squareup.anvil.annotations.ContributesMultibinding"

    }
}
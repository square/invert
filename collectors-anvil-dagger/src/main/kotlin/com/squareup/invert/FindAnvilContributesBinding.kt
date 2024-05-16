package com.squareup.invert

import com.rickbusarow.statik.InternalStatikApi
import com.rickbusarow.statik.element.kotlin.psi.utils.traversal.PsiTreePrinter
import com.squareup.psi.classesAndInnerClasses
import com.squareup.psi.requireFqName
import com.squareup.psi.toKtFile
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils.getLineAndColumnRangeInPsiFile
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import java.io.File


/**
 * Utility Class responsible for collecting all Anvil @ContributesBinding annotations
 * on classes in a Kotlin file.
 */
class FindAnvilContributesBinding {

    private val allBindings = mutableListOf<AnvilContributesBinding>()


    private val contributionAndConsumption = mutableListOf<AnvilContributionAndConsumption>()

    fun getCollectedContributionsAndConsumptions(): List<AnvilContributionAndConsumption> {
        return contributionAndConsumption
    }

    fun getCollectedContributesBindings(): List<AnvilContributesBinding> {
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
                ANVIL_CONTRIBUTES_BINDING_ANNOTATION_CLASS_NAME -> AnnotationInfo(
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

    fun handleKotlinFile(file: File) {
        val bindingsInFile = mutableListOf<AnvilContributesBinding>()
        if (file.exists()) {
            val ktFile = file.toKtFile()
            val fileName = file.name

            ktFile.accept(object : KtTreeVisitorVoid() {
                override fun visitConstructorDelegationCall(call: KtConstructorDelegationCall) {
                    super.visitConstructorDelegationCall(call)
                }
            })

            ktFile.classesAndInnerClasses()
                .toList()
                .forEach { ktClassOrObject ->
                    val bindingsInClassOrObject = mutableListOf<AnvilContributesBinding>()
                    val classOrObjectFqName = ktClassOrObject.fqName?.asString() ?: ktClassOrObject.name!!

                    val supertypes = ktClassOrObject.getSuperTypeList()?.entries
                        ?.map { it.requireFqName().asString() }
                    val annotationInfos = ktClassOrObject.extractAnnotationInfos()
                    val contributeBindingAnnotationInfo: AnnotationInfo? = annotationInfos
                        .firstOrNull { it.type == ANVIL_CONTRIBUTES_BINDING_ANNOTATION_CLASS_NAME }

                    contributeBindingAnnotationInfo?.let {
                        val scope = contributeBindingAnnotationInfo.args?.firstOrNull { it.name == "scope" }
                            ?: contributeBindingAnnotationInfo.args!!.first()

                        val boundType =
                            contributeBindingAnnotationInfo.args?.firstOrNull { it.name == "boundType" }
                        val replaces =
                            contributeBindingAnnotationInfo.args?.firstOrNull { it.name == "replaces" }

                        val anvilContributesBinding = AnvilContributesBinding(
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

                    val consumptions = findConstructorInjections(ktClassOrObject)


                    val lineAndColumnRange = getLineAndColumnRangeInPsiFile(ktFile, ktClassOrObject.textRange)
                    if (bindingsInClassOrObject.isNotEmpty() || consumptions.isNotEmpty()) {
                        contributionAndConsumption.add(
                            AnvilContributionAndConsumption(
                                fileName = fileName,
                                contributions = bindingsInClassOrObject,
                                consumptions = consumptions,
                                lineNumber = lineAndColumnRange.start.line,
                                classFqName = classOrObjectFqName,
                            )
                        )
                    }
                    bindingsInFile.addAll(bindingsInClassOrObject)
                }
        }
        allBindings.addAll(bindingsInFile)
    }

    private fun findConstructorInjections(ktClassOrObject: KtClassOrObject): List<AnvilInjection> {
        val anvilInjections = mutableListOf<AnvilInjection>()
        println("ktClassOrObject $ktClassOrObject")
        ktClassOrObject.primaryConstructor?.let { primaryConstructor ->
            val ktParameterList = primaryConstructor.findDescendantOfType<KtParameterList>()!!
            ktParameterList.forEachDescendantOfType<KtParameter> { ktParameter ->
                ktParameter.getChildOfType<KtTypeReference>().also { ktTypeReference ->
                    val paramFqName = ktTypeReference!!.requireFqName().asString()
                    println(paramFqName)
                    anvilInjections.add(
                        AnvilInjection(
                            type = paramFqName
                        )
                    )
                }
            }
        }
        return anvilInjections
    }

    companion object {
        const val ANVIL_CONTRIBUTES_BINDING_ANNOTATION_CLASS_NAME =
            "com.squareup.anvil.annotations.ContributesBinding"
    }
}
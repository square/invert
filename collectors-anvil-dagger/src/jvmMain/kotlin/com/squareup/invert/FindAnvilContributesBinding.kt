package com.squareup.invert

import com.squareup.psi.classesAndInnerClasses
import com.squareup.psi.requireFqName
import com.squareup.psi.toKtFile
import org.jetbrains.kotlin.psi.*
import java.io.File

/**
 * Utility Class responsible for collecting all Anvil @ContributesBinding annotations
 * on classes in a Kotlin file.
 */
class FindAnvilContributesBinding {

    private val allBindings = mutableListOf<AnvilContributesBinding>()

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

            ktFile.accept(object :KtTreeVisitorVoid() {
                override fun visitConstructorDelegationCall(call: KtConstructorDelegationCall) {
                    super.visitConstructorDelegationCall(call)
                }
            })

            ktFile.classesAndInnerClasses()
                .toList()
                .forEach { ktClassOrObject ->
                    val className = ktClassOrObject.fqName?.asString() ?: ktClassOrObject.name!!

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
                            boundImplementation = className,
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
                            fileName = fileName,
                        )
                        bindingsInFile.add(anvilContributesBinding)
                    }
                }
        }
        allBindings.addAll(bindingsInFile)
    }

    fun handleDirectory(directory: File) {
        if (directory.exists()) {
            directory.listFiles()?.forEach { childFile ->
                if (childFile.isDirectory) {
                    if (directory.name == "src" && childFile.name.contains("test", true)) {
                        // Ignoring test folder
                    } else if (directory.name == "build" && childFile.name == "anvil") {
                        // Ignoring Anvil build folder
                    } else {
                        handleDirectory(childFile)
                    }
                } else {
                    if (childFile.extension == "kt") {
                        handleKotlinFile(childFile)
                    }
                }
            }
        }
    }

    companion object {
        const val ANVIL_CONTRIBUTES_BINDING_ANNOTATION_CLASS_NAME =
            "com.squareup.anvil.annotations.ContributesBinding"
    }
}
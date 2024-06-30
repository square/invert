package com.squareup.af.analysis.stats.classdefinition

import com.squareup.af.analysis.models.KtClassInfo.Companion.toKtClassInfo
import com.squareup.af.analysis.models.KtClassInfo.Companion.toKtClassInfoData
import com.squareup.af.analysis.stats.KtClassInfoKeywordMatcher
import com.squareup.af.analysis.stats.classdefinition.model.ClassDefinitionInfo
import com.squareup.invert.CollectedStat
import com.squareup.invert.StatCollector
import com.squareup.invert.models.Stat.StringStat
import com.squareup.invert.models.StatMetadata
import com.squareup.psi.requireFqName
import com.squareup.psi.toKtFile
import org.jetbrains.kotlin.psi.KtClass
import java.io.File

open class SupertypeDefinitionStatCollector(
    private val statInfo: StatMetadata,
    val supertypes: Set<String>,
    val classMatchesExpectedType: KtClassInfoKeywordMatcher,
) : StatCollector {

    override fun collect(
        rootProjectFolder: File,
        projectPath: String,
        kotlinSourceFiles: List<File>,
    ): List<CollectedStat>? {
        val definedMatchingClasses = mutableListOf<ClassDefinitionInfo>()
        kotlinSourceFiles.forEach { file ->
            definedMatchingClasses.addAll(
                collectForClassDeclarations(
                    relativePath = file.absolutePath.replace(rootProjectFolder.absolutePath, "").drop(1),
                    supertypes = supertypes,
                    declaredKtClasses = file.toKtFile().declarations.filterIsInstance<KtClass>(),
                )
            )
        }
        return if (definedMatchingClasses.isNotEmpty()) {
            listOf(CollectedStat(statInfo, StringStat(definedMatchingClasses.joinToString("\n") { it.fqName })))
        } else {
            null
        }
    }

    override fun getName(): String {
        return this::class.java.name
    }

    /**
     * Given a list of declared classes in a given file, and a list of desired supertypes,
     * the implementer of this method should determine if the class declaration
     * should be collected based it their criteria (is abstract, interface, naming, etc).
     *
     * @return A [List] of [ClassDefinitionsStat]s with the definitions that
     * we should capture as part of this stat.
     */
    open fun collectForClassDeclarations(
        relativePath: String,
        supertypes: Set<String>,
        declaredKtClasses: List<KtClass>,
    ): List<ClassDefinitionInfo> {
        val collectedDefinitions = mutableListOf<ClassDefinitionInfo>()

        declaredKtClasses.forEach { ktClass: KtClass ->
            if (classMatchesExpectedType(ktClass.toKtClassInfoData())) {
                val className = ktClass.name.toString()
                val actualSupertypes = getDirectSupertypes(ktClass)
                if (actualSupertypes.any { supertypes.contains(it) }) {
                    collectedDefinitions.add(
                        ClassDefinitionInfo(
                            name = className,
                            fqName = ktClass.fqName?.asString() ?: className,
                            file = relativePath,
                            supertypes = actualSupertypes
                        )
                    )
                }
            }
        }
        return collectedDefinitions.toList()
    }

    /** These are direct supertypes only and does not reflect an entire class hierarchy */
    protected fun getDirectSupertypes(
        ktClass: KtClass
    ): List<String> = ktClass.getSuperTypeList()
        ?.entries
        ?.map { it.requireFqName().asString() } ?: listOf()
}

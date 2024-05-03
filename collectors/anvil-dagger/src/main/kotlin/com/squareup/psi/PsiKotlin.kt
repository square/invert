package com.squareup.psi

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtPureElement
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import java.io.File

fun File.toKtFile(
    content: String = readText(),
): KtFile {
    check(extension == "kt")
    return toKtFile(
        content = content,
        name = name,
    )
}

fun toKtFile(
    content: String,
    name: String = "untitled",
    psiFileFactory: PsiFileFactory = PSI_FILE_FACTORY
): KtFile {
    return psiFileFactory.createFileFromText(name, KotlinFileType.INSTANCE, content) as KtFile
}

fun KtFile.classesAndInnerClasses(): Sequence<KtClassOrObject> {
    val children = findChildrenByClass(KtClassOrObject::class.java)

    return generateSequence(children.toList()) { list ->
        list
            .flatMap {
                it.declarations.filterIsInstance<KtClassOrObject>()
            }
            .ifEmpty { null }
    }.flatMap { it.asSequence() }
}

fun KtClassOrObject.functions(): Sequence<KtFunction> =
    children
        .asSequence()
        .filterIsInstance<KtClassBody>()
        .flatMap {
            it.children
                .asSequence()
                .filterIsInstance<KtFunction>()
        }

fun KtAnnotated.isAnnotatedWith(fqName: FqName): Boolean {
    return annotations(fqName).isNotEmpty()
}

fun KtAnnotated.annotations(fqName: FqName): List<KtAnnotationEntry> {
    val fqNameString = fqName.asString()

    // If the simple name is used, check that the annotation is imported.
    val hasImport = containingKtFile.importDirectives
        .mapNotNull { it.importPath }
        .any {
            it.fqName == fqName
        }

    return annotationEntries
        .filter {
            // Check first if the fully qualified name is used, e.g. `@dagger.Module`.
            it.text.startsWith("@$fqNameString") ||
                // Check if the simple name is used, e.g. `@Module`.
                // Classes from the Kotlin namespace usually don't need to be imported, e.g. @PublishedApi.
                (it.shortName == fqName.shortName() && (hasImport || fqNameString.startsWith("kotlin.")))
        }
}

val PsiElement.fqNameSet: Set<FqName>
    get() = try {
        requireFqNameList().toSet()
    } catch (e: Exception) {
        setOf()
    }

fun PsiElement.requireFqName(): FqName {
    val fqNameList = requireFqNameList()
    when (fqNameList.size) {
        1 -> return fqNameList.first()
        else -> throw IllegalArgumentException("Multiple imports for $text")
    }
}

infix fun <TYPE> Set<TYPE>.doesIntersect(
    element: Set<TYPE>,
): Boolean = intersect(element).isNotEmpty()

infix fun <TYPE> Set<TYPE>.doesNotIntersect(
    element: Set<TYPE>,
): Boolean = !doesIntersect(element)

/**
* Tries to convert the given element into a FqName or throws an error. This function tries to make
* the best guess by looking at imports and other heuristics. However, there are some gaps that
* cannot be solved solely by Psi APIs, e.g. star imports, references in the same package, Kotlin
* types, etc. For that we need a real compiler to resolve types.
*
* For example:
* ```
* import mortar.Scoped
*
* @ContributesMultibinding(
*   scope = Unit::class,
*   boundType = Scoped::class
* )
* ```
* Calling this function for the Psi element `Scoped` would return the FqName `mortar.Scoped`.
*/

private fun PsiElement.requireFqNameList(): List<FqName> {
    val containingKtFile = parentsWithSelf
        .firstIsInstance<KtPureElement>()
        .containingKtFile

    fun failTypeHandling(): Nothing = throw IllegalArgumentException(
        "Don't know how to handle Psi element: $text"
    )

    val classReference = when (this) {
        // If a fully qualified name is used, then we're done and don't need to do anything further.
        is KtDotQualifiedExpression -> text
        is KtNameReferenceExpression -> getReferencedName()
        is KtUserType -> {
            val isGenericType = children.any { it is KtTypeArgumentList }
            if (isGenericType) {
                // For an expression like Lazy<Abc> the qualifier will be null. If the qualifier exists,
                // then it refers to package and the referencedName refers to the class name, e.g.
                // a KtUserType "abc.def.GenericType<String>" has three children: a qualifier "abc.def",
                // the referencedName "GenericType" and the KtTypeArgumentList.
                val packageName = qualifier?.text
                val className = referencedName

                if (packageName != null) {
                    return listOf(FqName("$packageName.$className"))
                }

                className ?: failTypeHandling()
            } else {
                text
            }
        }

        is KtTypeReference -> {
            val children = children
            if (children.size == 1) {
                try {
                    // Could be a KtNullableType or KtUserType.
                    return listOf(children[0].requireFqName())
                } catch (e: IllegalArgumentException) {
                    // Fallback to the text representation.
                    text
                }
            } else {
                text
            }
        }

        is KtNullableType -> return listOf(innerType?.requireFqName() ?: failTypeHandling())
        is KtAnnotationEntry -> return listOf(typeReference?.requireFqName() ?: failTypeHandling())
        is KtSuperTypeListEntry -> return listOf(typeReference?.requireFqName() ?: failTypeHandling())
        else -> failTypeHandling()
    }

    // E.g. OuterClass.InnerClass
    val classReferenceOuter = classReference.substringBefore(".")

    val importPaths = containingKtFile.importDirectives.mapNotNull { it.importPath }

    // First look in the imports for the reference name. If the class is imported, then we know the
    // fully qualified name.
    importPaths
        .filter { it.alias == null && it.fqName.shortName().asString() == classReference }
        .also { matchingImportPaths ->
            if (matchingImportPaths.isNotEmpty()) {
                return matchingImportPaths.map { it.fqName }
            }
        }

    importPaths
        .filter { it.alias == null && it.fqName.shortName().asString() == classReferenceOuter }
        .also { matchingImportPaths ->
            if (matchingImportPaths.isNotEmpty()) {
                return matchingImportPaths.map { it.fqName }
            }
        }

    // Check if it's a named import.
    containingKtFile.importDirectives
        .firstOrNull { classReference == it.importPath?.importedName?.asString() }
        ?.importedFqName
        ?.let { return listOf(it) }

    // At this point we need a compiler to resolve the type. But most frequently the class is in the
    // the same package, so use this as a default.
    //
    // It's a decent default, because most frequently we only analyze single files. For a class
    // reference the FqName would be wrong in the entire file, but it would always be the same FqName.
    // That's what we mostly care about.
    return listOf(containingKtFile.packageFqName.child(Name.identifier(classReference)))
}

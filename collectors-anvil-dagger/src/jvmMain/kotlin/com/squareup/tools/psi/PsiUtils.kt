//package com.squareup.tools.psi
//
//import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
//import org.jetbrains.kotlin.cli.common.messages.MessageCollector
//import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles.JVM_CONFIG_FILES
//import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
//import org.jetbrains.kotlin.com.intellij.ide.highlighter.JavaFileType
//import org.jetbrains.kotlin.com.intellij.mock.MockProject
//import org.jetbrains.kotlin.com.intellij.psi.PsiClass
//import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
//import org.jetbrains.kotlin.com.intellij.psi.impl.source.PsiJavaFileImpl
//import org.jetbrains.kotlin.config.CompilerConfiguration
//import org.jetbrains.kotlin.idea.KotlinFileType
//import org.jetbrains.kotlin.psi.KtClassOrObject
//import org.jetbrains.kotlin.psi.KtFile
//import java.io.File
//import kotlin.LazyThreadSafetyMode.NONE
//
//object PsiUtils {
//
//  /**
//   * This file is a collection of PSI Utilities to find the classes and inner classes
//   * of both Kotlin and Java files.  This is needed to help identify classes
//   * that are defined that do not match the file name, and in cases where multiple
//   * classes are defined in a single file.
//   *
//   * Original Source:
//   * https://github.com/squareup/android-register/tree/master/build-logic/conventions/psi
//   */
//  fun KtFile.classesAndInnerClasses(): Sequence<KtClassOrObject> {
//    val children = findChildrenByClass(KtClassOrObject::class.java)
//
//    return generateSequence(children.toList()) { list ->
//      list
//        .flatMap {
//          it.declarations.filterIsInstance<KtClassOrObject>()
//        }
//        .ifEmpty { null }
//    }.flatMap { it.asSequence() }
//  }
//
//  fun PsiJavaFileImpl.classesAndInnerClasses(): Sequence<PsiClass> {
//    val children = findChildrenByClass(PsiClass::class.java)
//
//    return generateSequence(children.toList()) { classes ->
//      classes
//        .flatMap {
//          it.innerClasses.asList()
//        }
//        .ifEmpty { null }
//    }.flatMap { it.asSequence() }
//  }
//
//  private val PSI_FILE_FACTORY by lazy(NONE) {
//    val project = KotlinCoreEnvironment.createForProduction(
//      parentDisposable = {},
//      configuration = CompilerConfiguration().apply {
//        put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
//      },
//      configFiles = JVM_CONFIG_FILES
//    ).project as MockProject
//
//    PsiFileFactory.getInstance(project)
//  }
//
//  fun File.toKtFile(): KtFile {
//    check(extension == "kt")
//    return PSI_FILE_FACTORY.createFileFromText(name, KotlinFileType.INSTANCE, readText()) as KtFile
//  }
//
//  fun File.toJavaFile(
//    content: String = readText(),
//    psiFileFactory: PsiFileFactory = PSI_FILE_FACTORY
//  ): PsiJavaFileImpl {
//    check(extension == "java")
//    return psiFileFactory.createFileFromText(name, JavaFileType.INSTANCE, content) as PsiJavaFileImpl
//  }
//
//  fun recursiveFindSrcFolder(
//    androidRegisterRootFolder: File,
//    parentFolder: File,
//  ): File? {
//    if (androidRegisterRootFolder == parentFolder) {
//      // Walked all the way back to the root of the project, so we could not determine it.
//      return null
//    }
//    return when (parentFolder.name) {
//      "kotlin", "java" -> {
//        parentFolder
//      }
//
//      else -> {
//        if (parentFolder.parentFile != null) {
//          recursiveFindSrcFolder(androidRegisterRootFolder, parentFolder.parentFile)
//        } else {
//          // Parent file is null so we could not determine it.
//          null
//        }
//      }
//    }
//  }
//
//  fun computeGradlePathFromSrcRoot(
//    registerRootProjectDirFolder: File,
//    srcRoot: File?
//  ): String? {
//    return srcRoot?.canonicalPath
//      ?.replace(registerRootProjectDirFolder.canonicalPath, "")
//      ?.replace("/", ":")
//  }
//
//  fun computeGradlePathFromFile(
//    registerRootProjectDirFolder: File,
//    srcRoot: File
//  ): String? {
//    val srcFolder = recursiveFindSrcFolder(
//      registerRootProjectDirFolder,
//      srcRoot
//    )
//    return computeGradlePathFromSrcRoot(
//      registerRootProjectDirFolder = registerRootProjectDirFolder,
//      srcRoot = srcFolder?.parentFile?.parentFile?.parentFile
//    )
//  }
//}

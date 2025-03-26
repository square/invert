#!/usr/bin/env kotlin

/**
 * This Gist is an example of how you can load Kotlin Files into Kotlin's K2 FIR Model.
 * This is written as a Kotlin Script (kts) an can be run on the commandline.
 *
 * Download this code to a file named "fir.main.kts".  Then run the following:
 * chmod +x fir.main.kts
 * ./fir.main.kts
 *
 * For help running kts scripts, see:
 *  https://kotlinlang.org/docs/command-line.html
 *  https://github.com/kscripting/kscript
 */

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.0.0")

import org.jetbrains.kotlin.KtVirtualFileSourceFile
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.GroupedKtSources
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.ModuleCompilerInput
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.compileModuleToAnalyzedFir
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.openapi.vfs.StandardFileSystems
import org.jetbrains.kotlin.com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.kotlin.com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.pipeline.FirResult
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import java.io.File
import java.nio.file.Files

/**
 * Loads classes using the compiler tools into the Frontend Intermediate Representation (FIR) for inspection.
 *
 * Based on:
 * https://github.com/cashapp/zipline/blob/4fa1014c833c46fd8c4b6b6add83786a2e4ea618/zipline-api-validator/src/main/kotlin/app/cash/zipline/api/validator/fir/KotlinFirLoader.kt
 * and
 * https://github.com/cashapp/redwood/blob/afe1c9f5f95eec3cff46837a4b2749cbaf72af8b/redwood-tooling-schema/src/main/kotlin/app/cash/redwood/tooling/schema/schemaParserFir.kt
 */
internal class KotlinFirLoader(
  private val sources: Collection<File>,
  private val classpath: Collection<File>,
) : AutoCloseable {
  private val disposable = Disposer.newDisposable()

  private val messageCollector = object : MessageCollector {
    override fun clear() = Unit
    override fun hasErrors() = false

    override fun report(
      severity: CompilerMessageSeverity,
      message: String,
      location: CompilerMessageSourceLocation?,
    ) {
      val destination = when (severity) {
        LOGGING -> null
        EXCEPTION, ERROR -> System.err
        else -> System.out
      }
      destination?.println(message)
    }
  }

  /**
   * @param targetName an opaque identifier for this operation.
   */
  fun load(targetName: String = "unnamed"): FirResult {
    val configuration = CompilerConfiguration()
    configuration.put(CommonConfigurationKeys.MODULE_NAME, targetName)
    configuration.put(CommonConfigurationKeys.USE_FIR, true)
    configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
    configuration.addKotlinSourceRoots(sources.map { it.absolutePath })
    configuration.addJvmClasspathRoots(classpath.toList())

    val environment = KotlinCoreEnvironment.createForProduction(
      disposable,
      configuration,
      EnvironmentConfigFiles.JVM_CONFIG_FILES,
    )
    val project = environment.project

    val localFileSystem = VirtualFileManager.getInstance().getFileSystem(
      StandardFileSystems.FILE_PROTOCOL,
    )
    val files = buildList {
      for (source in sources) {
        source.walkTopDown().filter { it.isFile }.forEach {
          this += localFileSystem.findFileByPath(it.absolutePath)!!
        }
      }
    }

    val sourceFiles = files.mapTo(mutableSetOf(), ::KtVirtualFileSourceFile)
    val input = ModuleCompilerInput(
      targetId = TargetId(JvmProtoBufUtil.DEFAULT_MODULE_NAME, targetName),
      groupedSources = GroupedKtSources(
        platformSources = sourceFiles,
        commonSources = emptyList(),
        sourcesByModuleName = mapOf(JvmProtoBufUtil.DEFAULT_MODULE_NAME to sourceFiles),
      ),
      commonPlatform = CommonPlatforms.defaultCommonPlatform,
      platform = JvmPlatforms.unspecifiedJvmPlatform,
      configuration = configuration,
    )

    val reporter = DiagnosticReporterFactory.createReporter()

    val globalScope = GlobalSearchScope.allScope(project)
    val packagePartProvider = environment.createPackagePartProvider(globalScope)
    val projectEnvironment = VfsBasedProjectEnvironment(
      project = project,
      localFileSystem = localFileSystem,
      getPackagePartProviderFn = { packagePartProvider },
    )

    return compileModuleToAnalyzedFir(
      input = input,
      projectEnvironment = projectEnvironment,
      previousStepsSymbolProviders = emptyList(),
      incrementalExcludesScope = null,
      diagnosticsReporter = reporter,
    )
  }

  override fun close() {
    disposable.dispose()
  }
}

/**
 * A made up data structure to hold collected data
 */
data class MyDeclaredClassData(
  val declarationName: String,
  val packageName: String,
  val startOffset: Int,
  val endOffset: Int,
)

/**
 * Loads all the Kotlin files recursively in a given directory
 *
 * @return All declared classes
 */
fun loadKotlinFilesInDir(dir: File) {
  println("Loading Kotlin Sources into FIR")
  val firLoader = KotlinFirLoader(
    sources = dir.walkTopDown()
      .filter { it.extension == "kt" }
      .toList(),
    classpath = emptyList(),
  )
  val output = firLoader.load("fir-dump")

  val platformOutput = output.outputs.first()

  // Go through the FIR for each file
  println("Iterating through FIR Data:")
  platformOutput.fir
    .forEach { firFile ->
      firFile.declarations.forEach { declaration ->
        when (declaration) {
          is FirRegularClass -> {
            val discoveredClass = MyDeclaredClassData(
              declarationName = declaration.name.asString(),
              packageName = firFile.packageFqName.asString(),
              startOffset = firFile.sourceFileLinesMapping?.getLineByOffset(declaration.source!!.startOffset)!!,
              endOffset = firFile.sourceFileLinesMapping?.getLineByOffset(declaration.source!!.endOffset)!!,
            )
            println("* $discoveredClass")
          }

          else -> {}
        }
      }
    }
}


/**
 * Generate 3 fake kotlin files for this Gist
 */
fun createDirectoryOfTempKotlinFiles(): File {
  // Create some temp sources for this gist
  val tempSrcDir: File = Files.createTempDirectory("fir-demo").toFile()
  (1..3).forEach { num ->
    File(tempSrcDir, "FirDemo$num.kt").also { newKotlinFile ->
      println("Creating Kotlin File at ${newKotlinFile.absolutePath}")
      buildString {
        appendLine("package com.handstandsam.fir.example")
        appendLine("class FirDemo$num {")
        appendLine("  fun someFunction$num() : Boolean { return true }")
        appendLine("}")
        appendLine()
      }.also { code ->
        newKotlinFile.writeText(code)
        println(code)
      }
    }
  }
  return tempSrcDir
}

// Replace with your source folder
val directoryWithKotlinFiles = createDirectoryOfTempKotlinFiles()

// Load the files into FIR and find the declared classes
loadKotlinFilesInDir(directoryWithKotlinFiles)
package com.squareup.invert

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.psi
import java.io.File


fun List<FirDeclaration>.findRegularClassesRecursive(): List<FirRegularClass> {
    val classes = filterIsInstance<FirRegularClass>()
    return classes + classes.flatMap { it.declarations.findRegularClassesRecursive() }
}

fun main() {
    val firLoader = KotlinFirLoader(
        sources = File("/Users/samedwards/Development/invert/collectors/collectors-anvil-dagger/src/").walkTopDown()
            .filter { it.extension == "kt" }
            .toList(),
        classpath = emptyList(),
    )
    val output = firLoader.load("fir-dump")
    println(output.toString())

    val platformOutput = output.outputs.first()

    val types = platformOutput.fir
        .forEach { firFile ->
            val filePath = firFile.sourceFile?.path
            println("TYPE: ${firFile}")
            firFile.sourceFile?.path
            firFile.declarations.forEach { declaration ->
                when (declaration) {
                    is FirSimpleFunction -> {
                        declaration.containerSource?.containingFile
                        declaration.body?.psi
                        println("PSI: " + declaration.source?.getElementTextInContextForDebug())
                        println("PSI: " + declaration.source?.startOffset)
//                        println("* Function Name: " + declaration.name)
                    }
                    else -> {}
                }
            }
        }


}
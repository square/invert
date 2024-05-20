package com.squareup.invert

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import java.io.File


fun List<FirDeclaration>.findRegularClassesRecursive(): List<FirRegularClass> {
    val classes = filterIsInstance<FirRegularClass>()
    return classes + classes.flatMap { it.declarations.findRegularClassesRecursive() }
}

fun main() {
    val firLoader = KotlinFirLoader(
        javaHome = File("/Users/samedwards/Library/Java/JavaVirtualMachines/azul-17-ARM64/Contents/Home"),
        jdkRelease = 17,
        sources = File("/Users/samedwards/Development/invert/collectors-anvil-dagger/src/test/kotlin/com/squareup/invert/").listFiles()
            .toList(),
        classpath = emptyList(),
    )
    val output = firLoader.load("fir-dump")
    println(output.toString())

    val platformOutput = output.outputs.first()
    val session: FirSession = platformOutput.session
    println("builtinTypes ${session.builtinTypes}")
    println("Session ${session.kind}")

    val types = platformOutput.fir
        .flatMap { it.declarations.findRegularClassesRecursive() }
        .forEach { type ->
            println("TYPE: ${type.name}")
            type.declarations.forEach { declaration ->
                when(declaration) {
                    is FirSimpleFunction -> {
                        println("* Function Name: " + declaration.name)
                    }
                    else ->{}
                }
            }
        }


}
/**
 * Implementation from https://github.com/pgreze/kowners
 */
package com.github.pgreze.kowners

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

fun File.lsFiles(target: File? = null): List<String> =
    listOfNotNull("git", "ls-files", target?.toString())
        .runCommand(this)
        .split('\n')
        .filter { it.isNotBlank() }

// https://stackoverflow.com/a/41495542
@Throws(IOException::class)
private fun List<String>.runCommand(workingDir: File, timeOutSeconds: Long = 10): String {
    val proc = ProcessBuilder(filter { it.isNotBlank() })
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()

    proc.waitFor(timeOutSeconds, TimeUnit.SECONDS)
    return proc.inputStream.bufferedReader().readText()
}

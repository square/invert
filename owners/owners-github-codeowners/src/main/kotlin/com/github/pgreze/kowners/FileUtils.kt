package com.github.pgreze.kowners

import java.io.File

fun File.findCodeOwnerLocations(): List<File> =
    CODEOWNERS_LOCATIONS
        .map { File(this, "$it/$CODEOWNERS_FILENAME") }
        .filter { it.exists() }

internal val CODEOWNERS_LOCATIONS = arrayOf(
    ".",
    "docs",
    ".github"
)
internal const val CODEOWNERS_FILENAME = "CODEOWNERS"

/** Return files (not folder) related to [base] */
fun File.listFilesRecursively(base: File = this): List<File> =
    listFiles()!!.toList()
        .sortedWith(Comparator(File::compareWithDirectoryFirst))
        .flatMap {
            it.takeUnless(File::isDirectory)?.let { f -> listOf(f.relativeTo(base)) }
                ?: it.listFilesRecursively(base)
        }

private fun File.compareWithDirectoryFirst(other: File): Int =
    when {
        isDirectory == other.isDirectory -> compareTo(other)
        isDirectory -> -1 // Display directories first
        else -> +1
    }
